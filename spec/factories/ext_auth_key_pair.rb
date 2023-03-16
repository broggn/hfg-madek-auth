require 'open3'

AuthSystem.class_eval do
  attr_accessor :external_private_key
end

FactoryBot.define do
  factory :auth_system do
    type {"external"}
    name {Faker::Company.name}
    id {name.gsub(/\s+/, '-').downcase}

    transient do
      internal_key { ECKey.new }
      external_key { ECKey.new }
    end

    internal_private_key { internal_key.private_key }
    internal_public_key { internal_key.public_key }

    external_private_key { external_key.private_key }
    external_public_key { external_key.public_key }
  end
end
