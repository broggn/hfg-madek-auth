require 'spec_helper'

feature 'Sesssion expiration'  do

  before :each do 
    @user = FactoryBot.create :user
  end

  scenario 'works even if changed after creation of the session' do

    # first we let the session expire after one hour
    ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc
      UPDATE auth_systems SET session_max_lifetime_hours = 1
    SQL


    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email', with: @user.email
    click_on 'Continue'
    click_on 'Madek Password Authentication'
    fill_in :password, with: @user.password
    click_on 'Submit'
    # we are on the supplied return-to path:
    uri = Addressable::URI.parse(current_url)
    expect(uri.path).to be== '/auth/info'

    expect{find("code.user-session-data")}.not_to raise_error 
    expect{YAML.load(find("code.user-session-data").text)}.not_to raise_error 
    user_session_data = YAML.load(find("code.user-session-data").text).with_indifferent_access
    session_expires_at = Time.parse(user_session_data[:session_expires_at])

    # we can see, that the session expires in about one hour
    expect(Time.now() + 1.hour - 1.minute).to be<= session_expires_at
    expect(Time.now() + 1.hour + 1.minute).to be>= session_expires_at


    # now we let the session expire within the next 30 seconds after creation
    ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc
      UPDATE auth_systems SET session_max_lifetime_hours = 30.0 / (60 * 60)
    SQL

    # after reload we can see, that the session expires within the next minute 
    visit current_url
    expect{find("code.user-session-data")}.not_to raise_error 
    expect{YAML.load(find("code.user-session-data").text)}.not_to raise_error 
    user_session_data = YAML.load(find("code.user-session-data").text).with_indifferent_access
    session_expires_at = Time.parse(user_session_data[:session_expires_at])
    expect(Time.now() + 1.minute).to be>= session_expires_at

    # we wait for the session to expire and after reload we can not see any auth info anymore
    sleep(31)
    visit current_url
    expect{find("code.user-session-data")}.to raise_error 


  end
end
