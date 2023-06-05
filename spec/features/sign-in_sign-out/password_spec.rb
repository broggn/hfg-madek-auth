require 'spec_helper'

feature 'Sign in /out with password'  do

  before :each do 
    @user = FactoryBot.create :user
  end

  scenario 'Sign in with proper password works' do
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email', with: @user.email
    click_on 'Continue'
    click_on 'Madek Password Authentication'
    fill_in :password, with: @user.password
    click_on 'Submit'
    uri = Addressable::URI.parse(current_url)
    # we are on the supplied return-to path:
    expect(uri.path).to be== '/auth/info'

    expect{find("code.user-session-data")}.not_to raise_error 
    expect{YAML.load(find("code.user-session-data").text)}.not_to raise_error 
    user_session_data = YAML.load(find("code.user-session-data").text).with_indifferent_access
    expect(user_session_data[:person_last_name]).to be== @user.person.last_name
    user_session_id = user_session_data[:session_id]
    expect(user_session_id).to be
    expect{UserSession.find(user_session_id)}.not_to raise_error

    click_on @user.person.last_name
    find("form button", text: 'Sign out').click
    expect(current_path).to be== '/'
    expect{UserSession.find(user_session_id)}.to raise_error ActiveRecord::RecordNotFound
  end


  scenario 'Sign in wrong password fails' do
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email', with: @user.email
    click_on 'Continue'
    click_on 'Madek Password Authentication'
    fill_in :password, with: "foo"
    click_on 'Submit'
    ActiveRecord::Base.connection 
    expect(page).to have_content "Request ERROR 401"
    expect(page).to have_content "Password missmatch"
    visit '/auth/info'
    expect{find("code.user-session-data")}.to raise_error 
    expect(UserSession.all()).to be_empty
  end

  scenario 'Expired password does not show in available auth systems' do
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc
      UPDATE auth_systems_users SET expires_at = now() - Interval '1 hour'
      WHERE auth_systems_users.auth_system_id = 'password'
      AND auth_systems_users.user_id = '#{@user.id}'
    SQL
    fill_in 'email', with: @user.email
    click_on 'Continue'
    expect(page).to have_content \
      "Sign-in or sign-up is not available for this e-mail address."
  end

  scenario 'Sign in with expired password fails' do
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email', with: @user.email
    click_on 'Continue'
    click_on 'Madek Password Authentication'
    ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc
      UPDATE auth_systems_users SET expires_at = now() - Interval '1 hour'
      WHERE auth_systems_users.auth_system_id = 'password'
      AND auth_systems_users.user_id = '#{@user.id}'
    SQL
    fill_in :password, with: @user.password
    click_on 'Submit'
    expect(page).to have_content "Request ERROR 401"
    expect(page).to have_content "Password authentication is not available"
    visit '/auth/info'
    expect{find("code.user-session-data")}.to raise_error 
    expect(UserSession.all()).to be_empty
  end



end
