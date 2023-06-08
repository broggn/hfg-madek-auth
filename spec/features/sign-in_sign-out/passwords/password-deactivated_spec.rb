require 'spec_helper'

feature 'Sign in with deactivated account'  do

  before :each do 
    @user = FactoryBot.create :user
  end

  def deactivate_user
    ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc
      UPDATE users SET is_deactivated = true
      WHERE users.id = '#{@user.id}'
    SQL
  end

  scenario 'Deactivated account does not password authentication in available auth systems' do
    deactivate_user
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email', with: @user.email
    click_on 'Continue'
    expect(page).to have_content \
      "Sign-in or sign-up is not available for this e-mail address."
  end

  scenario 'Sign in with deactivated account fails' do
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email', with: @user.email
    click_on 'Continue'
    click_on 'Madek Password Authentication'
    deactivate_user
    fill_in :password, with: @user.password
    click_on 'Submit'
    expect(page).to have_content "Request ERROR 401"
    expect(page).to have_content "Password authentication is not available"
    visit '/auth/info'
    expect{find("code.user-session-data")}.to raise_error 
    expect(UserSession.all()).to be_empty
  end

end
