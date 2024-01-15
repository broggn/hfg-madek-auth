require 'spec_helper'

feature 'Sign in with expired password'  do

  before :each do 
    @user = FactoryBot.create :user
  end

  scenario 'Expired password does not show in available auth systems' do
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc
      UPDATE auth_systems_users SET expires_at = now() - Interval '1 hour'
      WHERE auth_systems_users.auth_system_id = 'password'
      AND auth_systems_users.user_id = '#{@user.id}'
    SQL
    fill_in 'email-or-login', with: @user.email
    click_on 'Weiter'
    expect(page).to have_content \
      "Einloggen nicht möglich mit dieser E-Mail"
  end

  scenario 'Sign in with expired password fails' do
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email-or-login', with: @user.email
    click_on 'Weiter'
    ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc
      UPDATE auth_systems_users SET expires_at = now() - Interval '1 hour'
      WHERE auth_systems_users.auth_system_id = 'password'
      AND auth_systems_users.user_id = '#{@user.id}'
    SQL
    fill_in :password, with: @user.password
    click_on 'Anmelden'
    expect(page).to have_content "Request ERROR 401"
    expect(page).to have_content "Password authentication is not available"
    visit '/auth/info'
    expect{find("code.user-session-data")}.to raise_error 
    expect(UserSession.all()).to be_empty
  end

end
