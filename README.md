# Triton

[![Spigot](https://img.shields.io/badge/dynamic/json?color=blue&label=Spigot&prefix=v&query=%24.current_version&url=https%3A%2F%2Fapi.spigotmc.org%2Fsimple%2F0.2%2Findex.php%3Faction%3DgetResource%26id%3D30331)](https://www.spigotmc.org/resources/triton-translate-your-server.30331/)
[![Spigot Rating](https://img.shields.io/spiget/rating/30331?color=orange)](https://www.spigotmc.org/resources/triton-translate-your-server.30331/)
[![Release](https://jitpack.io/v/diogotcorreia/Triton.svg)](https://jitpack.io/#tritonmc/Triton)

_Translate your server! Sends the same message in different languages... Hooks into all plugins!_  
This repository was previously called MultiLanguagePlugin.

Triton is a Minecraft plugin for Spigot/Paper, BungeeCord, and Velocity that helps you translate your Minecraft server!

Purchase the plugin on [Spigot](https://spigotmc.org/resources/triton.30331/)
or [Polymart](https://polymart.org/resource/triton.38)!

## Using the API

The recommended way to use Triton's API is through the Gradle/Maven artifact.
Note that the Maven repository mentioned below is only available since
Triton v3.11.2.

<details>
<summary>Gradle (Groovy) Instructions</summary>

Firstly, add the following repository to your project:

```groovy
repositories {
    maven {
        url "https://repo.diogotc.com/releases"
    }
}
```

Then, you should be able to add the Triton API dependency.
Make sure to NOT shade it into your plugin by using `compileOnly`.

```groovy
dependencies {
    // change the version to whatever the latest one is
    compileOnly "com.rexcantor64.triton:triton-api:4.0.0"
}
```
</details>

<details>
<summary>Maven Instructions</summary>

Firstly, add the following repository to your project:

```xml
<repository>
  <id>diogotc-repository-releases</id>
  <name>Diogo Correia's Releases Repository</name>
  <url>https://repo.diogotc.com/releases</url>
</repository>
```

Then, you should be able to add the Triton API dependency.
Make sure to NOT shade it into your plugin by setting the appropriate `scope`.

```xml
<dependency>
  <groupId>com.rexcantor64.triton</groupId>
  <artifactId>triton-api</artifactId>
  <!-- change the version to whatever the latest one is -->
  <version>4.0.0</version>
  <scope>provided</scope>
</dependency>
```
</details>

Need help developing? Take a look at the
[wiki](https://github.com/tritonmc/Triton/wiki),
[JavaDocs](https://triton.rexcantor64.com/javadocs) or join
our [Discord](https://triton.rexcantor64.com/discord)!

Looking for older API versions? Take a look at
the [download page](https://github.com/diogotcorreia/Triton/wiki/Downloads)
or [JitPack](https://jitpack.io/#tritonmc/triton/).

## Compiling from Source

Triton is still a premium plugin and if you're going to use it,
it's advised that you purchase it from Spigot or Polymart, as stated above.  
Nevertheless, you're still free to compile it yourself if you have the skills to do so.  
No support will be given to self-compiled versions.

To compile, clone this repository and run the following command:

```sh
./gradlew shadowJar
```

## MiniMessage & Security Features

Triton includes support for Kyori's Adventure MiniMessage format natively in translations.

### Features
- **Auto-Detection**: Strings containing standard MiniMessage tags (e.g. `<green>`, `<gradient:red:blue>`, hex codes) are automatically detected and parsed as MiniMessage without requiring any prefixes.
- **Prefixes**: Explicitly enforce formats by prefixing translation values with `[minimsg]` for MiniMessage or `[triton_json]` for raw JSON components.
- **Triton Custom Tag**: Use the `<triton:key>` tag in MiniMessage templates to import/embed another translation raw value into the current template (useful for shared color palettes or reusable formats).

### Security & Safe Translations
To prevent click-action (command execution) injection or structure exploits from player-provided inputs/arguments (e.g., chat messages or player names inside placeholders):
- **Click Event Stripping**: When `safe-translations` is enabled in `config.yml`, click actions are completely stripped from translation arguments. If the original translation template doesn't define click events, all click events will also be stripped from the final formatted message.
- **Delimiter Sanitization**: Internal parser delimiter characters (used to demarcate styles/events recursively) are scrubbed from arguments to prevent delimiter-injection attacks.

### Bedrock Edition & Geyser/Floodgate Translation Bridge

Triton includes native support for translating custom player GUI forms sent to Bedrock Edition players connecting via Geyser or Floodgate.
- **Interception**: Geyser and Floodgate custom GUI Forms (e.g., SimpleForm, CustomForm, ModalForm) bypass standard Java Edition packet interception. Triton hooks into `GeyserApi` and `FloodgateApi` dynamically using dynamic reflection proxies to intercept outgoing forms.
- **Recursive Translation**: Triton automatically traverses the form object graph recursively, translating all string elements (such as form titles, description texts, input placeholders, buttons, and options lists) matching Triton's translation formats (like `[lang]key[/lang]`) into the player's selected language.
- **Automatic Integration**: No extra configuration is required. The bridge is activated automatically if Geyser or Floodgate is detected on the server.

### Java/Bedrock Platform Variants

Triton can send different text to Java Edition and Bedrock Edition players without changing the player's selected language.
- **Storage**: Platform variants are loaded from the folder configured under `platform-variants.folder`, which defaults to `platforms`. Every `.json` file in that folder is loaded, not only `default.json`.
- **Usage**: Use `[plat]example.variant[/plat]`, `[plat]example.variant[arg]value[/arg][/plat]`, or `%triton_plat_example.variant%`.
- **Formatting**: Variant values support the same formatting pipeline as translations, including legacy colors, MiniMessage auto-detection, `[minimsg]`, `[triton_json]`, arguments, nested `[lang]key[/lang]` placeholders, and safe-translation protections.

Example `plugins/Triton/platforms/default.json`:

```json
{
  "items": [
    {
      "type": "platform",
      "key": "example.variant",
      "variants": {
        "java": "&aJava player text with %1.",
        "bedrock": "&bBedrock player text with %1."
      }
    }
  ]
}
```

---

# Triton (Türkçe)

Triton, Minecraft sunucunuzu çevirmenize yardımcı olan Spigot/Paper, BungeeCord ve Velocity için geliştirilmiş bir Minecraft eklentisidir! Aynı mesajı farklı dillerdeki oyunculara kendi dillerinde gönderir ve tüm eklentilerle entegre çalışır.

Eklentiyi [Spigot](https://spigotmc.org/resources/triton.30331/) veya [Polymart](https://polymart.org/resource/triton.38) üzerinden satın alabilirsiniz.

## API Kullanımı

Triton API'sini kullanmanın önerilen yolu Gradle/Maven kütüphanelerini eklemektir. Aşağıda belirtilen Maven deposu Triton v3.11.2 sürümünden itibaren kullanılabilir.

<details>
<summary>Gradle (Groovy) Yönergeleri</summary>

Öncelikle projenize aşağıdaki depoyu (repository) ekleyin:

```groovy
repositories {
    maven {
        url "https://repo.diogotc.com/releases"
    }
}
```

Ardından Triton API bağımlılığını ekleyebilirsiniz. `compileOnly` kullanarak eklentinizi göerken API'yi gömmediğinizden (shade etmediğinizden) emin olun.

```groovy
dependencies {
    // Sürümü en güncel olanla değiştirin
    compileOnly "com.rexcantor64.triton:triton-api:4.0.1-fork"
}
```
</details>

<details>
<summary>Maven Yönergeleri</summary>

Öncelikle projenize aşağıdaki depoyu ekleyin:

```xml
<repository>
  <id>diogotc-repository-releases</id>
  <name>Diogo Correia's Releases Repository</name>
  <url>https://repo.diogotc.com/releases</url>
</repository>
```

Ardından Triton API bağımlılığını ekleyebilirsiniz. `scope` değerini `provided` olarak ayarlayarak API'yi gömmediğinizden emin olun.

```xml
<dependency>
  <groupId>com.rexcantor64.triton</groupId>
  <artifactId>triton-api</artifactId>
  <!-- Sürümü en güncel olanla değiştirin -->
  <version>4.0.1-fork</version>
  <scope>provided</scope>
</dependency>
```
</details>

Geliştirme konusunda yardıma mı ihtiyacınız var? [Wiki](https://github.com/tritonmc/Triton/wiki) sayfamıza, [JavaDocs](https://triton.rexcantor64.com/javadocs) dokümanlarımıza göz atın veya [Discord](https://triton.rexcantor64.com/discord) sunucumuza katılın!

Eski API sürümlerini mi arıyorsunuz? [İndirme sayfası](https://github.com/diogotcorreia/Triton/wiki/Downloads) veya [JitPack](https://jitpack.io/#tritonmc/triton/) adresini inceleyin.

## Kaynaktan Derleme

Triton ücretli (premium) bir eklentidir ve kullanacaksanız Spigot veya Polymart üzerinden satın almanız önerilir. Yine de, kendi başınıza derlemek istiyorsanız derlemekte özgürsünüz. Kendi derlediğiniz sürümler için destek sağlanmamaktadır.

Derlemek için bu depoyu klonlayın ve aşağıdaki komutu çalıştırın:

```sh
./gradlew shadowJar
```

## MiniMessage ve Güvenlik Özellikleri

Triton, çevirilerde Kyori Adventure MiniMessage formatını yerel olarak destekler.

### Özellikler
- **Otomatik Algılama (Auto-Detection)**: Standart MiniMessage etiketlerini (örn. `<green>`, `<gradient:red:blue>`, hex renk kodları) içeren metinler otomatik olarak algılanır ve herhangi bir ön ek (prefix) gerekmeden MiniMessage olarak çözümlenir.
- **Ön Ekler**: Çeviri değerlerinin başına MiniMessage için `[minimsg]` veya ham JSON bileşenleri için `[triton_json]` ekleyerek belirli formatları zorunlu kılabilirsiniz.
- **Özel Triton Etiketi**: MiniMessage şablonlarında `<triton:key>` etiketini kullanarak başka bir çeviriyi mevcut şablonun içerisine gömebilirsiniz (renk paletleri veya ortak şablonlar için kullanışlıdır).

### Güvenlik ve Güvenli Çeviriler (Safe Translations)
Oyuncu girdilerinden (argümanlar) kaynaklanabilecek click action (komut yürütme) enjeksiyonlarını ve yapısal suistimalleri önlemek için:
- **Tıklama Eylemi Temizleme (Click Event Stripping)**: `config.yml` dosyasında `safe-translations` aktif olduğunda, çevirilerin içerisindeki dinamik oyuncu argümanlarından tüm tıklama eylemleri tamamen temizlenir. Eğer orijinal çeviri şablonunda herhangi bir tıklama eylemi tanımlanmadıysa, güvenlik amacıyla nihai mesajdaki tüm tıklama eylemleri temizlenir.
- **Sınırlayıcı Temizliği (Delimiter Sanitization)**: Stil ve olayları iç içe çözümlerken kullanılan Triton eklentisine ait dahili sınırlayıcı karakterler (`\uE400` - `\uE802`), enjeksiyon saldırılarını önlemek amacıyla argümanlardan tamamen filtrelenir.

### Bedrock Edition ve Geyser/Floodgate Çeviri Köprüsü

Triton, Geyser veya Floodgate aracılığıyla bağlanan Bedrock Edition oyuncularına gönderilen özel arayüz formlarının çevrilmesini yerel olarak destekler.
- **Form Yakalama (Interception)**: Geyser ve Floodgate'in özel GUI Formları (SimpleForm, CustomForm, ModalForm), standart Java Edition paket yakalama işlemlerini bypass eder. Triton, form gönderimlerini anlık olarak yakalamak için `GeyserApi` ve `FloodgateApi` sınıflarına çalışma zamanında (runtime) yansıma (reflection) tabanlı dinamik proxyler enjekte eder.
- **Rekürsif Çeviri**: Form başlıkları, içerik yazıları, buton isimleri, girdi alanları (input placeholders) ve açılır menü seçenekleri gibi tüm metin öğeleri otomatik olarak taranır ve oyuncunun seçtiği dile göre Triton çeviri formatları (`[lang]anahtar[/lang]`) kullanılarak rekürsif (iç içe) olarak çevrilir.
- **Otomatik Entegrasyon**: Herhangi bir ek ayar gerektirmez. Geyser veya Floodgate sunucuda algılandığında çeviri köprüsü otomatik olarak devreye girer.
