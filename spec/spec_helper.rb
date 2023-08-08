require 'pathname'
PROJECT_DIR = Pathname.new(__FILE__).join('../..').realpath

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

  config.after :each do |example|
    if example.exception 
      if ENV['PRY_ON_EXCEPTION'].presence
        logger.error example.exception
        binding.pry
      else
        logger.warn("set PRY_ON_EXCEPTION to stop and pry after a failed example")
      end
    end
  end
end
