require 'capybara/rspec'
require 'faker'
require 'logger'
require 'pry'
require 'rspec'

require 'lib/crypto'

require 'config/rails'
require 'config/database'
require 'config/browser'


$logger = logger = Logger.new(STDOUT)
logger.level = Logger::INFO

RSpec.configure do |config|

  config.before :all do
    @spec_seed = \
      ENV['SPEC_SEED'].presence.try(:strip) || `git log -n1 --format=%T`.strip
    $logger.info "SPEC_SEED='#{@spec_seed}'"
    srand Integer(@spec_seed, 16)
  end

  config.after :all do
    puts
    $logger.info "SPEC_SEED='#{@spec_seed}'"
  end

  #config.include FactoryBot::Syntax::Methods
end


