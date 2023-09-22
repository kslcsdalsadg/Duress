# Duress

Duress password trigger.

<img 
     src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" 
     width="30%" 
     height="30%">

Tiny app to listen for a duress password on the lockscreen.  
When found, it can send a broadcast message or wipe the device.

Also take a look at:
* [Wasted](https://github.com/x13a/Wasted)
* [Sentry](https://github.com/x13a/Sentry)

Be aware that the app does not work in _safe mode_.

## Tested

* Google Pixel 7Pro with GrapheneOS, Android 13

## Permissions

* ACCESSIBILITY - listen for a duress password on the lockscreen
* DEVICE_ADMIN - wipe the device (optional)

## Localization

[<img 
     height="51" 
     src="https://badges.crowdin.net/badge/dark/crowdin-on-light@2x.png" 
     alt="Crowdin">](https://crwd.in/me-lucky-duress)

## Related

* [pam_duress](https://github.com/rafket/pam_duress)
* [pam_panic](https://github.com/pampanic/pam_panic)
* [pam-party](https://github.com/x13a/pam-party)
* [lockup](https://github.com/nekohasekai/lockup)
* [pam-duress](https://github.com/nuvious/pam-duress)

## License

[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](https://www.gnu.org/licenses/gpl-3.0.en.html)
