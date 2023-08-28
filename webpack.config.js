const MiniCssExtractPlugin = require('mini-css-extract-plugin')

module.exports = {
  mode: 'development',
  entry: {
    'main': './css-src/main.scss'
  },
  output: {
    path: __dirname + '/resources/auth/public/css',
    filename: '[name].js'
  },
  devtool: 'source-map',
  optimization: {},
  module: {
    rules: [
      {
        test: /\.scss$/,
        use: [
          MiniCssExtractPlugin.loader,
          'css-loader',
          {
            loader: 'postcss-loader', // Run post css actions
            options: {
              postcssOptions: {
                plugins: [['autoprefixer', {}]]
              }
            }
          },
          'sass-loader'
        ]
      },

      // plain css
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader']
      },

      // fonts
      {
        test: /\.(woff|woff2)$/,
        type: 'asset/resource'
      }
    ]
  },
  plugins: [new MiniCssExtractPlugin({ filename: '[name].css' })]
}
