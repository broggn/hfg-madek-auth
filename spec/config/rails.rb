require 'rails/all'
require 'factory_bot_rails'

ENV['RAILS_ENV'] = ENV['RAILS_ENV'].presence || 'test'

module Madek
  class Application < Rails::Application
    config.eager_load = false

    config.autoload_paths += [
      Rails.root.join('datalayer', 'lib'),
      Rails.root.join('datalayer', 'app', 'models'),
      Rails.root.join('datalayer', 'app', 'lib')
    ]

    config.paths['config/database'] = ['datalayer/config/database.yml']
    config.factory_bot.definition_file_paths << 'datalayer/spec/factories'

  end
end


Rails.application.initialize!
