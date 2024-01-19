require 'spec_helper'

feature 'Sign in / sign out via ext auth with management', ci_group: :extauth do

  let :ext_auth_port do
    ENV['TEST_AUTH_SYSTEM_PORT'] || '3167'
  end

  let :ext_auth_key_pair do
    ECKey.new
  end

  let :ext_auth_id do
    'ext-account-manage-auth-sys'
  end

  let :ext_auth_name do
    'External Account Management Authentication System'
  end

  before :each do

    @domain = Faker::Internet.domain_name
    @first_name = Faker::Name.first_name
    @last_name = Faker::Name.last_name
    @institutional_id = Faker::Internet.uuid
    @login = (@last_name + @first_name).downcase.gsub(/[^a-z]/,'').truncate(8, omission: '')
    @email = @first_name + " " + @last_name + "@" + @domain

    File.write(PROJECT_DIR.join("tmp/ext_auth_account.yml"),
               { email: @email,
                 first_name: @first_name,
                 id: @institutional_id,
                 last_name: @last_name,
                 login: @login,
                 groups: [
                   {institutional_id: "g001",
                    name: "Group g001",
                    type: 'InstitutionalGroup',
                    institutional_name: "Inst Grp g001"},
                    {institutional_id: "g002",
                     name: "Group g002",
                     type: 'InstitutionalGroup',
                     institutional_name: "Inst Grp g002"},
                     {institutional_id: "g003",
                      name: "Group g003",
                      type: 'InstitutionalGroup',
                      institutional_name: "Inst Grp g003"},
                      {institutional_id: "all",
                       name: "All users from this auth system",
                       type: 'AuthenticationGroup' }
                 ]
               }.as_json.to_yaml)

    @auth_system = FactoryBot.create :auth_system,
      id: ext_auth_id,
      name: ext_auth_name,
      external_sign_in_url: "http://localhost:#{ext_auth_port}/sign-in",
      external_public_key: ext_auth_key_pair.public_key,
      external_private_key: nil,
      manage_accounts: true,
      managed_domain: @domain,
      email_or_login_match: ".*@" + @domain,
      enabled: true

    File.write(PROJECT_DIR.join('./tmp/private_key.pem'), ext_auth_key_pair.private_key)
    File.write(PROJECT_DIR.join('./tmp/public_key.pem'), @auth_system.internal_public_key)



  end

  scenario 'Create account and sign-in ' do

    visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
    fill_in 'email-or-login', with: @email
    click_on 'Weiter'
    # click_on ext_auth_name # -> app does redirect automatically

    within('div', text: 'Authentication with User-Management Properties') do
      click_on "I am"
    end

    # redirecting and full reload takes some time; somewhat dirty but more easy
    # to debug than wait_until
    sleep(0.5)
    uri = Addressable::URI.parse(current_url)
    # we are on the supplied return-to path:
    expect(uri.path).to be== '/auth/info'

    @user=User.first

    # check user including full name was found in db
    expect(@user).to be
    expect(@user.last_name).not_to be_empty
    expect(@user.first_name).not_to be_empty

    # check some content:
    expect{find("code.user-session-data")}.not_to raise_error
    expect{YAML.load(find("code.user-session-data").text)}.not_to raise_error
    user_session_data = YAML.load(find("code.user-session-data").text).with_indifferent_access
    expect(user_session_data[:user_last_name]).to be== @user.last_name
    expect(user_session_data[:user_first_name]).to be== @user.first_name
    user_session_id = user_session_data[:session_id]
    expect(user_session_id).to be
    expect{UserSession.find(user_session_id)}.not_to raise_error

    # check person which was created along with user (not visible in UI)
    expect(@user.last_name).to be== @user.person.last_name
    expect(@user.first_name).to be== @user.person.first_name

    click_on @user.last_name
    find("form button", text: 'Abmelden').click
    expect(current_path).to be== '/'
    expect{UserSession.find(user_session_id)}.to raise_error ActiveRecord::RecordNotFound

  end

  context 'Update account and sign-in' do

    before :each do
      @user = FactoryBot.create :user,
        institutional_id: @institutional_id,
        institution: @domain

      group = FactoryBot.create :group,
        institution: @domain,
        institutional_id: "100",
        name: "user should not be in group"

      @user.groups << group

    end

    scenario 'it works' do
      visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
      fill_in 'email-or-login', with: @email
      click_on 'Weiter'
      # click_on ext_auth_name # -> app does redirect automatically

      within('div', text: 'Authentication with User-Management Properties') do
        click_on "I am"
      end

      # redirecting and full reload takes some time; somewhat dirty but more easy
      # to debug than wait_until
      sleep(0.5)
      uri = Addressable::URI.parse(current_url)
      # we are on the supplied return-to path:
      expect(uri.path).to be== '/auth/info'

      # user and person's name before they are update
      expect(@user.last_name).not_to be== @last_name
      expect(@user.person.last_name).not_to be== @last_name
      person_last_name_old = @user.person.last_name

      # validate updated properties
      @user.reload
      expect(@user.last_name).to be== @last_name
      expect(@user.first_name).to be== @first_name
      expect(@user.email).to be== @email
      expect(@user.login).to be== @login

      # person's last_name must remain as it was before login
      expect(@user.person.last_name).to be== person_last_name_old

    end

    scenario 'it updates groups, adds and removes user from apropiate groups' do
      visit '/auth/sign-in?return-to=%2Fauth%2Finfo&foo=42'
      fill_in 'email-or-login', with: @email
      click_on 'Weiter'
      # click_on ext_auth_name # -> app does redirect automatically

      within('div', text: 'Authentication with User-Management Properties') do
        click_on "I am"
      end

      # redirecting and full reload takes some time; somewhat dirty but more easy
      # to debug than wait_until
      sleep(0.5)
      uri = Addressable::URI.parse(current_url)
      # we are on the supplied return-to path:
      expect(uri.path).to be== '/auth/info'

      # the four we created plus the special madek group
      expect(@user.reload.groups.reload.map(&:id).count).to be== 5


      # audits

      audited_records = ActiveRecord::Base.connection.execute <<-SQL.strip_heredoc
        SELECT * FROM audited_requests
        JOIN audited_responses ON audited_requests.txid = audited_responses.txid
        LEFT JOIN audited_changes ON audited_changes.txid = audited_requests.txid
        ORDER BY audited_requests.created_at ASC;
      SQL

    end

  end

end
