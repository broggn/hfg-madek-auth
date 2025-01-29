require 'spec_helper'

describe 'ECKey' do
  it "can be instantiated without error and has initilized keys" do
    binding.pry
    expect{ @ec_key = Madek::Crypto::ECKey.new}.not_to raise_error
    expect(@ec_key).to be
    expect(@ec_key.private_key).to be_present
    expect(@ec_key.public_key).to be_present
  end
end

