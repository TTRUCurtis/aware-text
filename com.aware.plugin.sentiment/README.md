AWARE Plugin: Sentimental
==========================

[![Release](https://jitpack.io/v/denzilferreira/com.aware.plugin.sentimental.svg)](https://jitpack.io/#denzilferreira/com.aware.plugin.sentimental)

This plugin performs lexical analysis on user input text.

Lexica are dictionaries associating words with categories; word-category association is optionally weighted. They provide a simple but effective way to analyze the content of a piece of text.

This plugin captures user keyboard input and applies standard lexica to the text. Input can be captured from specific apps, such as WhatsApp or the Android messaging app, or it can be captured universally.

# Settings

Parameters adjustable on the dashboard and client:
- **status_plugin_sentimental**: (boolean) activate/deactivate plugin
- **plugin_sentimental_packages**: (String) com.whatsapp, com.google.android.apps.messaging, ... If left empty, the plugin will be used in any app that accepts text input

# Lexica

At present, the best way to add new lexica is to change the file at `src/main/res/raw/sentiment.json` and recompile the AWARE client. We plan to make lexica downloadable in a future update.

# Credits

This plugin was developed by researchers at the University of Pennsylvania.
