require 'spec_helper'

feature 'Sign in /out with password'  do

  before :each do 
    @user = FactoryBot.create :user
  end

  scenario 'Sign in with proper password works and is audited' do
    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email-or-login', with: @user.email
    click_on 'Weiter'
    fill_in :password, with: @user.password
    click_on 'Anmelden'
    uri = Addressable::URI.parse(current_url)
    # we are on the supplied return-to path:
    expect(uri.path).to be== '/auth/info'

    expect{find("code.user-session-data")}.not_to raise_error 
    expect{YAML.load(find("code.user-session-data").text)}.not_to raise_error 
    user_session_data = YAML.load(find("code.user-session-data").text).with_indifferent_access
    expect(user_session_data[:user_last_name]).to be== @user.last_name
    user_session_id = user_session_data[:session_id]
    expect(user_session_id).to be
    expect{UserSession.find(user_session_id)}.not_to raise_error

    click_on @user.last_name
    find("form button", text: 'Abmelden').click
    expect(current_path).to be== '/'
    expect{UserSession.find(user_session_id)}.to raise_error ActiveRecord::RecordNotFound


    # audits

    audited_records = ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc 
      SELECT * FROM audited_requests
      JOIN audited_responses ON audited_requests.txid = audited_responses.txid
      LEFT JOIN audited_changes ON audited_changes.txid = audited_requests.txid
      ORDER BY audited_requests.created_at ASC;
    SQL

    # sign in
    expect(audited_records[0].with_indifferent_access[:status]).to be== 200
    expect(audited_records[0].with_indifferent_access[:method]).to be== 'POST'
    expect(audited_records[0].with_indifferent_access[:table_name]).to be== 'user_sessions'
    expect(audited_records[0].with_indifferent_access[:tg_op]).to be== 'INSERT'

    # sign out
    expect(audited_records[1].with_indifferent_access[:status]).to be== 204
    expect(audited_records[1].with_indifferent_access[:method]).to be== 'POST'
    expect(audited_records[1].with_indifferent_access[:table_name]).to be== 'user_sessions'
    expect(audited_records[1].with_indifferent_access[:tg_op]).to be== 'DELETE'

  end


  scenario 'Sign in with wrong password fails' do

    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email-or-login', with: @user.email
    click_on 'Weiter'
    fill_in :password, with: "foo"
    click_on 'Anmelden'
    ActiveRecord::Base.connection 
    expect(page).to have_content "Falscher Benutzername/Passwort"
    visit '/auth/info'
    expect{find("code.user-session-data")}.to raise_error 
    expect(UserSession.all()).to be_empty

    # audits

    audited_records = ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc 
      SELECT * FROM audited_requests
      JOIN audited_responses ON audited_requests.txid = audited_responses.txid
      LEFT JOIN audited_changes ON audited_changes.txid = audited_requests.txid
      ORDER BY audited_requests.created_at ASC;
    SQL

    expect(audited_records[0].with_indifferent_access[:status]).to be== 401
    expect(audited_records[0].with_indifferent_access[:method]).to be== 'POST'

  end

end
