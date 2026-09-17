import js from "@eslint/js";
import vue from "eslint-plugin-vue";
import globals from "globals";
import tseslint from "typescript-eslint";
export default [
  {
    ignores: [
      "dist/",
      "node_modules/",
      "coverage/",
      "playwright-report/",
      "public/mockServiceWorker.js",
    ],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...vue.configs["flat/recommended"],
  {
    files: ["**/*.vue"],
    languageOptions: { parserOptions: { parser: tseslint.parser } },
  },
  {
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
    rules: {
      "@typescript-eslint/no-explicit-any": "error",
      // Prettier owns SFC layout; these stylistic rules otherwise conflict with its output.
      "vue/max-attributes-per-line": "off",
      "vue/singleline-html-element-content-newline": "off",
      "vue/multiline-html-element-content-newline": "off",
      "vue/html-closing-bracket-newline": "off",
      "vue/html-indent": "off",
      "vue/html-self-closing": "off",
    },
  },
  {
    // Registry components intentionally mirror their upstream source. Their
    // public names and prop-forwarding patterns do not follow app-level rules.
    files: [
      "src/shared/ui/**/*.vue",
      "src/shared/attachments/**/*.vue",
      "src/shared/conversation/**/*.vue",
      "src/shared/inline-citation/**/*.vue",
      "src/shared/loader/**/*.vue",
      "src/shared/message/**/*.vue",
      "src/shared/prompt-input/**/*.vue",
      "src/shared/shimmer/**/*.vue",
      "src/shared/suggestion/**/*.vue",
    ],
    rules: {
      "vue/multi-word-component-names": "off",
      "@typescript-eslint/no-empty-object-type": "off",
      "@typescript-eslint/no-explicit-any": "off",
      "@typescript-eslint/no-unused-vars": "off",
      "no-undef": "off",
      "vue/require-default-prop": "off",
    },
  },
];
