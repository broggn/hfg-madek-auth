require 'active_support/all'
require 'addressable/uri'
require 'haml'
require 'jwt'
require 'optparse'
require 'pathname'
require 'pry'
require 'sinatra/base'
require 'yaml'

$logger = logger = Logger.new(STDOUT)
logger.level = Logger::INFO


PROJECT_DIR = Pathname.new(__FILE__).join('../..').realpath

### opts #######################################################################

$options = {
  port: ENV['PORT'].presence || '3167',
  private_key_file: Pathname.new(ENV['PRIVATE_KEY_FILE'].presence || "tmp/private_key.pem" ),
  public_key_file: Pathname.new(ENV['PUBLIC_KEY_FILE'].presence || "tmp/public_key.pem" )
}


def parse
  OptionParser.new do |parser|
    parser.banner = "test-auth-system [options]"

    parser.on("--public-key-file=PUBLIC_KEY_FILE") do |fn|
      $options[:public_key_file] = Pathname.new(fn)
    end

    parser.on("--private-key-file=PRIVATE_KEY_FILE") do |fn|
      $options[:private_key_file] = Pathname.new(fn)
    end

    parser.on("--port=PORT") do |p|
      $options[:port] = p
    end

    parser.on("-h", "--help", "Print help") do
      puts parser
      puts "current options:"
      puts $options
      exit 0
    end
  end.parse!
end

parse



### key helpers ################################################################


def public_key
  OpenSSL::PKey.read(IO.read($options[:public_key_file]))
end

def private_key
  OpenSSL::PKey.read(IO.read($options[:private_key_file]))
end

### account ####################################################################

def account_properties
  begin
    YAML.load_file(PROJECT_DIR.join("tmp/ext_auth_account.yml"))
  rescue
    $logger.warn("tmp/ext_auth_account.yml does not exist, falling back to empty account")
    {}
  end

end

### service ####################################################################

class MadekAuthService < Sinatra::Application
  set :port, $options[:port]

  get '/' do
    'Hello world!'
  end


  get '/sign-in' do
    sign_in_request_token = params[:token]
    # TODO do verify, catch and redirect back with error
    token_data = JWT.decode sign_in_request_token, public_key, true, { algorithm: 'ES256' }
    email_or_login = token_data.first["email-or-login"]

    success_token = JWT.encode({
      sign_in_request_token: sign_in_request_token,
      'email-or-login': email_or_login,
      success: true}, private_key, 'ES256')

    fail_token = JWT.encode({
      sign_in_request_token: sign_in_request_token,
      error_message: "The user did not authenticate successfully!"}, private_key, 'ES256')


    managed_account_success_token = JWT.encode({
      sign_in_request_token: sign_in_request_token,
      'email-or-login': email_or_login,
      'account': account_properties(),
      success: true}, private_key, 'ES256')



    url = token_data.first["sign-in-url"]

    html =
      Haml::Template.new() do
        <<-HAML.strip_heredoc
          %h1 The Super Secure Test Authentication System

          %div

            %h2 Just Authentication

            %p
              Answer truthfully! Are you
              %em
                #{email_or_login}
              ?
            %ul
              %li
                %a{href: "#{url}?token=#{success_token}"}
                  %span
                    Yes, I am
                    %em
                      #{email_or_login}
              %li
                %a{href: "#{url}?token=#{fail_token}"}
                  %span
                    No, I am not
                    %em
                      #{email_or_login}

          %div

            %h2 Authentication with User-Management Properties

            %p
              Answer truthfully! Are you
              %em
                #{email_or_login}
              ?
            %ul
              %li
                %a{href: "#{url}?token=#{managed_account_success_token}"}
                  %span
                    I am
                    %em>
                      #{email_or_login}
                    %span<>
                      please create or update my account



        HAML
      end.render
      html
  end

  run!
end



