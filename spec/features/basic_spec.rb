require 'spec_helper'

feature 'Basic' do
  scenario 'Factories work' do
    created_user = FactoryBot.create :user
    user = User.find_by(id: created_user.id)
    expect(user).to be
  end

  scenario 'ExtAuthSystem factory works' do
    auth_system = FactoryBot.create :auth_system
  end

end
