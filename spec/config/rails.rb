require 'rails/all'

ENV['RAILS_ENV'] = ENV['RAILS_ENV'].presence || 'test'

module Madek
  class Application < Rails::Application
    config.eager_load = false

    config.paths['config/initializers'] << Rails.root.join('datalayer', 'initializers')

    config.autoload_paths += [
      Rails.root.join('datalayer', 'lib'),
      Rails.root.join('datalayer', 'app', 'models', 'concerns'),
      Rails.root.join('datalayer', 'app', 'models'),
      Rails.root.join('datalayer', 'app', 'lib')
    ]

    config.paths['config/database'] = ['datalayer/config/database.yml']
    
    Rails.application.config.active_record.legacy_connection_handling = false
    Rails.autoloaders.main.inflector.inflect("json" => "JSON")
  end
end

Rails.application.initialize!
