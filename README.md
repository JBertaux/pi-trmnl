# pi-trmnl

![PiTrmnl Screen](examples/example-1.png?raw=true "PiTrmnl Screen")

Python script to push your Pi-Hole stats to a custom TRMNL plugin

## How to run it ?

### Edit your crontab file

```bash
crontab -e
```

### Cron configuration

```txt
0 */6 * * * java -jar build/libs/pi-trmnl-1.0.0-fat.jar -e <PIHOLE-ENDPOINT> -p <PIHOLE-PASSWORD> -t <TRMNL-PLUGIN-ID> > ~/crontab_log.txt
```
