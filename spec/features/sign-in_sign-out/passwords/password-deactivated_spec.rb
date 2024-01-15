require 'spec_helper'

feature 'Sign in with deactivated account'  do

  before :each do 
    @user = FactoryBot.create :user
  end

  def deactivate_user
    ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc
      UPDATE users SET active_until = current_date - interval '1 day'
      WHERE users.id = '#{@user.id}'
    SQL
  end

  scenario 'Deactivated account does not password authentication in available auth systems' do
    deactivate_user
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email-or-login', with: @user.email
    click_on 'Weiter'
    expect(page).to have_content \
      "Einloggen nicht möglich mit dieser E-Mail"
  end

  scenario 'Sign in with deactivated account fails' do
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email-or-login', with: @user.email
    click_on 'Weiter'
    deactivate_user
    fill_in :password, with: @user.password
    click_on 'Anmelden'
    expect(page).to have_content "Request ERROR 401"
    expect(page).to have_content "Password authentication is not available"
    visit '/auth/info'
    expect{find("code.user-session-data")}.to raise_error 
    expect(UserSession.all()).to be_empty
  end

end
