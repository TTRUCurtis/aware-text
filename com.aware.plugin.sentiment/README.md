AWARE Plugin: Sentiment
=======================

[![Release](https://jitpack.io/v/denzilferreira/com.aware.plugin.sentiment.svg)](https://github.com/wwbp/com.aware.plugin.sentiment)

This plugin performs lexical analysis on user input text.

Lexica are dictionaries associating words with categories; word-category association is optionally weighted. They provide a simple but effective way to analyze the content of a piece of text.

This plugin captures user keyboard input and applies standard lexica to the text. Input can be captured from specific apps, such as WhatsApp or the Android messaging app, or it can be captured universally.

# Compilation

This is a plugin, not a stand-alone module and requires the main aware client to compile.  
If you clone the repository in wwbp/aware-client (https://github.com/wwbp/aware_client) this submodule will come with it.
Please see the compilation and apk construction instructions there.

# Settings

Parameters adjustable on the dashboard and client:
- **status_plugin_sentiment**: (boolean) activate/deactivate plugin
- **plugin_sentiment_packages**: (String) com.whatsapp, com.google.android.apps.messaging, ... If left empty, the plugin will be used in any app that accepts text input

# Default Lexica

By default, this plugin will produce scores for happiness, life satisfaction, stress, loneliness, affect, politness, and pronouns. Please use the following citations if you use these measures in your work:

```
Affect
------
Preoţiuc-Pietro, D., Schwartz, H. A., Park, G., Eichstaedt, J., Kern, M., Ungar, L., & Shulman, E. (2016, June). Modelling valence and arousal in facebook posts. In Proceedings of the 7th workshop on computational approaches to subjectivity, sentiment and social media analysis (pp. 9-15).

Happiness
---------
Giorgi, S., Guntuku, S. C., Eichstaedt, J. C., Pajot, C., Schwartz, H. A., & Ungar, L. H. (2021, May). Well-Being Depends on Social Comparison: Hierarchical Models of Twitter Language Suggest That Richer Neighbors Make You Less Happy. In Proceedings of the International AAAI Conference on Web and Social Media (Vol. 15, pp. 1069-1074).

Life Satisfaction
-----------------
Jaidka, K., Giorgi, S., Schwartz, H. A., Kern, M. L., Ungar, L. H., & Eichstaedt, J. C. (2020). Estimating geographic subjective well-being from Twitter: A comparison of dictionary and data-driven language methods. Proceedings of the National Academy of Sciences, 117(19), 10165-10171.

Loneliness
----------
Guntuku, S. C., Schneider, R., Pelullo, A., Young, J., Wong, V., Ungar, L., ... & Merchant, R. (2019). Studying expressions of loneliness in individuals using twitter: an observational study. BMJ open, 9(11), e030355.

Politeness
----------
Li, M., Hickman, L., Tay, L., Ungar, L., & Guntuku, S. C. (2020). Studying Politeness across Cultures Using English Twitter and Mandarin Weibo. Proceedings of the ACM on Human-Computer Interaction, 4(CSCW2), 1-15.

Stress
------
Guntuku, S. C., Buffone, A., Jaidka, K., Eichstaedt, J. C., & Ungar, L. H. (2019, July). Understanding and measuring psychological stress using social media. In Proceedings of the International AAAI Conference on Web and Social Media (Vol. 13, pp. 214-225).
```

# Adding Custom Lexica

At present, the best way to add new lexica is to change the file at `src/main/res/raw/sentiment.json` and recompile the AWARE client. 


Below is a snippet of the default json file (we only include two entries under `words`). Custom lexica must adhere to the same json format, in that you must have the following json keys:

* categories: a list of categories in your lexicon
* words: a dictionary keyed on words where the value is another dictionary keyed on categories whose value are the weights

In this example, the word `spiders` is in the stress, life satifaction, loneliness, and happiness categories with various weights (e.g., -2.17 in the stress category). Note, it is required that (1) both the keys `categories` and `words` are lowercase and (2) all words in the `words` category are lowercase. 

```
{
    "categories": [
        "stress", 
        "loneliness", 
        "politness", 
        "pronouns", 
        "affect", 
        "life_satisfaction", 
        "happiness"
    ],
    "words": {
        "woods": {
            "stress": -0.0409033194103, 
            "life_satisfaction": 1.28024853502, 
            "loneliness": -1.5188139304e-05, 
            "happiness": -0.108791104746
        }, 
        "spiders": {
            "stress": -2.17482227055, 
            "life_satisfaction": -57.1726185212, 
            "loneliness": 3.80655292379e-06, 
            "happiness": -59.8970483102
        }, 
    ...
}
```

# Credits

This plugin was developed by researchers at the University of Pennsylvania.
