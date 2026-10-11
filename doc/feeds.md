Sample RSS Feeds
================

International Feeds
-------------------

Android Weekly
http://us2.campaign-archive1.com/feed?u=887caf4f48db76fd91e20a06d&amp;id=4eb677ad19

Sammobile
https://www.sammobile.com/feed/


German Feeds
------------

Heise
http://www.heise.de/newsticker/heise-atom.xml

Stadt Bremerhaven
http://feeds.feedburner.com/stadt-bremerhaven/dqXM?format=xml

ComputerBase
http://www.computerbase.de/rss/news.xml


Private feeds (HTTP Basic Auth)
-------------------------------

Some feeds sit behind HTTPS and require HTTP Basic Authentication (for example a CloudFront distribution with edge auth).

1. Add the feed with an **https://** URL only (do not embed `user:password@` in the URL).
2. Open the feed settings and fill in **Username** and **Password** (both optional fields, but at least one must be set to use stored credentials).
3. Credentials are kept in the on-device feed database, not in the URL, OPML export, or settings backup JSON.
4. On refresh, the app sends `Authorization: Basic …` only to the **same HTTPS host** as the feed URL (not to other hosts after redirects, HTML feed discovery, or favicon fetches).


Per-feed refresh options
--------------------------

**Exclude from automatic refresh** skips the feed when the global timer runs and when **Refresh on open** runs. You can still update it with long-press → **Refresh** on that feed, or with **Refresh all** on the overview (which intentionally includes excluded feeds).

**Refresh only over Wi‑Fi** skips the feed on mobile data during any automatic job that respects Wi‑Fi rules.

**Offline feed** does not change RSS polling. After a normal refresh, the background job downloads **full article text** for entries that do not have it yet, using the feed’s default viewer mode (Feed, Readability, Readability4J, etc.). That text is stored on the device so articles can be read with less network use later. With **Readability4J** as the default, opening an item uses the cached full text when available (no article HTTP fetch). Offline feeds also appear in the **Offline** entry filter alongside top feeds. Expect extra data and battery use on feeds with Readability4J enabled.

RSS refresh HTTP uses OkHttp with Datadog RUM instrumentation when diagnostics are enabled (same as Readability4J article fetches).


[back to Readme](..//README.md)
