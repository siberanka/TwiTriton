package com.rexcantor64.triton.bridge;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.FloodgateApi;

import java.lang.reflect.*;
import java.util.*;

public class BedrockBridge {

    private static boolean hasGeyser = false;
    private static boolean hasFloodgate = false;

    public static void init() {
        try {
            Class.forName("org.geysermc.geyser.api.GeyserApi");
            hasGeyser = true;
        } catch (ClassNotFoundException ignored) {}
        
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            hasFloodgate = true;
        } catch (ClassNotFoundException ignored) {}
        
        if (!hasGeyser && !hasFloodgate) {
            Triton.get().getLogger().logInfo("Neither Geyser nor Floodgate detected on classpath. Bedrock translation bridge disabled.");
            return;
        }
        
        Triton.get().getLogger().logInfo("Bedrock translation bridge enabled (Geyser: " + hasGeyser + ", Floodgate: " + hasFloodgate + "). Hooking APIs...");

        final boolean geyserNeeded = hasGeyser;
        final boolean floodgateNeeded = hasFloodgate;

        Thread hookThread = new Thread(() -> {
            try {
                int attempts = 0;
                boolean geyserHooked = false;
                boolean floodgateHooked = false;
                
                while (attempts < 60) {
                    if (geyserNeeded && !geyserHooked) {
                        try {
                            if (org.geysermc.api.Geyser.isRegistered()) {
                                hookGeyser();
                                geyserHooked = true;
                            }
                        } catch (Throwable ignored) {}
                    }
                    
                    if (floodgateNeeded && !floodgateHooked) {
                        try {
                            Field apiField = org.geysermc.floodgate.api.InstanceHolder.class.getDeclaredField("api");
                            apiField.setAccessible(true);
                            if (apiField.get(null) != null) {
                                hookFloodgate();
                                floodgateHooked = true;
                            }
                        } catch (Throwable ignored) {}
                    }
                    
                    boolean finishedGeyser = !geyserNeeded || geyserHooked;
                    boolean finishedFloodgate = !floodgateNeeded || floodgateHooked;
                    if (finishedGeyser && finishedFloodgate) {
                        break;
                    }
                    
                    attempts++;
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ignored) {
            } catch (Throwable t) {
                Triton.get().getLogger().logError(t, "Error in BedrockBridge hook thread:");
            }
        });
        
        hookThread.setName("Triton-BedrockBridge-Hook-Thread");
        hookThread.setDaemon(true);
        hookThread.start();
    }

    private static void hookGeyser() {
        try {
            Field apiField = org.geysermc.api.Geyser.class.getDeclaredField("api");
            apiField.setAccessible(true);
            Object originalApi = apiField.get(null);
            if (originalApi != null && !Proxy.isProxyClass(originalApi.getClass())) {
                Object proxyApi = Proxy.newProxyInstance(
                    originalApi.getClass().getClassLoader(),
                    new Class<?>[] { GeyserApi.class },
                    new GeyserApiHandler(originalApi)
                );
                apiField.set(null, proxyApi);
                Triton.get().getLogger().logInfo("Successfully hooked GeyserApi!");
            }
        } catch (Throwable t) {
            Triton.get().getLogger().logError(t, "Failed to hook GeyserApi:");
        }
    }

    private static void hookFloodgate() {
        try {
            Field apiField = org.geysermc.floodgate.api.InstanceHolder.class.getDeclaredField("api");
            apiField.setAccessible(true);
            Object originalApi = apiField.get(null);
            if (originalApi != null && !Proxy.isProxyClass(originalApi.getClass())) {
                Object proxyApi = Proxy.newProxyInstance(
                    originalApi.getClass().getClassLoader(),
                    new Class<?>[] { FloodgateApi.class },
                    new FloodgateApiHandler(originalApi)
                );
                apiField.set(null, proxyApi);
                Triton.get().getLogger().logInfo("Successfully hooked FloodgateApi!");
            }
        } catch (Throwable t) {
            Triton.get().getLogger().logError(t, "Failed to hook FloodgateApi:");
        }
    }

    private static Object createConnectionProxy(Object realConnection) {
        if (realConnection == null) return null;
        if (Proxy.isProxyClass(realConnection.getClass())) return realConnection;
        
        return Proxy.newProxyInstance(
            realConnection.getClass().getClassLoader(),
            new Class<?>[] { GeyserConnection.class, Connection.class },
            new GeyserConnectionHandler(realConnection)
        );
    }

    private static class GeyserApiHandler implements InvocationHandler {
        private final Object realApi;

        public GeyserApiHandler(Object realApi) {
            this.realApi = realApi;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result;
            try {
                result = method.invoke(realApi, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            
            if (result != null) {
                if (method.getName().equals("connectionByUuid") || method.getName().equals("connectionByXuid")) {
                    return createConnectionProxy(result);
                } else if (method.getName().equals("onlineConnections")) {
                    List<?> originalList = (List<?>) result;
                    List<Object> proxiedList = new ArrayList<>();
                    for (Object conn : originalList) {
                        proxiedList.add(createConnectionProxy(conn));
                    }
                    return proxiedList;
                }
            }
            return result;
        }
    }

    private static class GeyserConnectionHandler implements InvocationHandler {
        private final Object realConnection;

        public GeyserConnectionHandler(Object realConnection) {
            this.realConnection = realConnection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("sendForm") && args.length == 1) {
                Object formArg = args[0];
                try {
                    UUID playerUuid = (UUID) realConnection.getClass().getMethod("javaUuid").invoke(realConnection);
                    translateObject(playerUuid, formArg, Collections.newSetFromMap(new IdentityHashMap<>()));
                } catch (Throwable t) {
                    Triton.get().getLogger().logError(t, "Error translating Geyser form:");
                }
            }
            try {
                return method.invoke(realConnection, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    private static class FloodgateApiHandler implements InvocationHandler {
        private final Object realApi;

        public FloodgateApiHandler(Object realApi) {
            this.realApi = realApi;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("sendForm") && args.length == 2) {
                UUID playerUuid = (UUID) args[0];
                Object formArg = args[1];
                try {
                    translateObject(playerUuid, formArg, Collections.newSetFromMap(new IdentityHashMap<>()));
                } catch (Throwable t) {
                    Triton.get().getLogger().logError(t, "Error translating Floodgate form:");
                }
            }
            try {
                return method.invoke(realApi, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    public static Object translateObject(UUID playerUuid, Object obj, Set<Object> visited) {
        if (obj == null) return null;
        if (obj instanceof String) {
            return translateString(playerUuid, (String) obj);
        }
        
        if (visited.contains(obj)) return obj;
        visited.add(obj);
        
        Class<?> clazz = obj.getClass();
        String pkg = clazz.getPackage() != null ? clazz.getPackage().getName() : "";
        if (pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("sun.") || pkg.startsWith("com.sun.")) {
            if (obj instanceof List) {
                List<Object> list = (List<Object>) obj;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    Object translated = translateObject(playerUuid, item, visited);
                    if (translated != item) {
                        try {
                            list.set(i, translated);
                        } catch (UnsupportedOperationException ignored) {}
                    }
                }
            } else if (obj instanceof Map) {
                Map<Object, Object> map = (Map<Object, Object>) obj;
                for (Map.Entry<Object, Object> entry : new ArrayList<>(map.entrySet())) {
                    Object key = entry.getKey();
                    Object val = entry.getValue();
                    Object transKey = translateObject(playerUuid, key, visited);
                    Object transVal = translateObject(playerUuid, val, visited);
                    if (transKey != key || transVal != val) {
                        try {
                            map.remove(key);
                            map.put(transKey, transVal);
                        } catch (UnsupportedOperationException ignored) {}
                    }
                }
            }
            return obj;
        }
        
        // Safety check: only walk fields of Geyser, Floodgate, or Cumulus classes/interfaces
        if (!isGeyserOrCumulusClass(clazz)) {
            return obj;
        }
        
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value != null) {
                        if (value instanceof String) {
                            String translated = translateString(playerUuid, (String) value);
                            if (!translated.equals(value)) {
                                field.set(obj, translated);
                            }
                        } else {
                            translateObject(playerUuid, value, visited);
                        }
                    }
                } catch (Throwable ignored) {}
            }
            currentClass = currentClass.getSuperclass();
        }
        return obj;
    }

    private static boolean isGeyserOrCumulusClass(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) return false;
        String name = clazz.getName();
        if (name.startsWith("org.geysermc.cumulus.") || 
            name.startsWith("org.geysermc.geyser.") || 
            name.startsWith("org.geysermc.floodgate.")) {
            return true;
        }
        if (isGeyserOrCumulusClass(clazz.getSuperclass())) {
            return true;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (isGeyserOrCumulusClass(iface)) {
                return true;
            }
        }
        return false;
    }

    private static String translateString(UUID playerUuid, String text) {
        if (text == null || text.isEmpty()) return text;
        TritonLanguagePlayer<?> lp = Triton.get().getPlayerManager().get(playerUuid);
        if (lp == null) return text;
        
        try {
            com.rexcantor64.triton.api.language.TranslationResult<String> result = Triton.get().getMessageParser().translateString(
                text, lp, Triton.get().getConfig().getChatSyntax()
            );
            if (result.isChanged()) {
                return result.getResultRaw();
            }
        } catch (Throwable ignored) {}
        return text;
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (uuid == null) return false;
        if (hasFloodgate) {
            try {
                if (org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(uuid)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        if (hasGeyser) {
            try {
                if (org.geysermc.api.Geyser.isRegistered() && org.geysermc.geyser.api.GeyserApi.api().isBedrockPlayer(uuid)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static boolean openLanguageSelectionForm(UUID uuid) {
        if (!isBedrockPlayer(uuid)) {
            return false;
        }
        try {
            return BedrockFormSender.openForm(uuid);
        } catch (Throwable t) {
            Triton.get().getLogger().logError(t, "Failed to open language selection form for Bedrock player " + uuid);
            return false;
        }
    }

    private static class BedrockFormSender {
        public static boolean openForm(UUID uuid) {
            TritonLanguagePlayer<?> lp = Triton.get().getPlayerManager().get(uuid);
            if (lp == null) return false;

            // 1. Get title
            String titleString = "Select a language";
            try {
                net.kyori.adventure.text.Component titleComp = Triton.get().getMessagesConfig().getMessageComponent("other.selector-gui-name");
                com.rexcantor64.triton.api.language.TranslationResult<net.kyori.adventure.text.Component> titleTrans = Triton.get().getMessageParser().translateComponent(
                    titleComp, lp, Triton.get().getConfig().getGuiSyntax()
                );
                net.kyori.adventure.text.Component finalTitle = titleTrans.isChanged() ? titleTrans.getResultRaw() : titleComp;
                if (finalTitle != null) {
                    titleString = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(finalTitle);
                }
            } catch (Throwable ignored) {}

            // 2. Get description/content
            String descString = "";
            try {
                net.kyori.adventure.text.Component descComp = Triton.get().getMessagesConfig().getMessageComponent("other.selector-gui-description");
                com.rexcantor64.triton.api.language.TranslationResult<net.kyori.adventure.text.Component> descTrans = Triton.get().getMessageParser().translateComponent(
                    descComp, lp, Triton.get().getConfig().getGuiSyntax()
                );
                net.kyori.adventure.text.Component finalDesc = descTrans.isChanged() ? descTrans.getResultRaw() : descComp;
                if (finalDesc != null) {
                    String serialized = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(finalDesc);
                    if (!serialized.equals("Unknown message")) {
                        descString = serialized;
                    }
                }
            } catch (Throwable ignored) {}

            // 3. Build form
            org.geysermc.cumulus.form.SimpleForm.Builder builder = org.geysermc.cumulus.form.SimpleForm.builder();
            builder.title(titleString);
            if (!descString.isEmpty()) {
                builder.content(descString);
            }

            final List<com.rexcantor64.triton.language.Language> languages = new ArrayList<>();
            for (com.rexcantor64.triton.api.language.Language lang : Triton.get().getLanguageManager().getAllLanguages()) {
                languages.add((com.rexcantor64.triton.language.Language) lang);
                
                String countryCode = getCountryCode(lang);
                String btnText = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(((com.rexcantor64.triton.language.Language) lang).getDisplayNameComponent());
                
                boolean isCurrent = lp.getLang().equals(lang);
                if (isCurrent) {
                    btnText = "§a✔ §r" + btnText;
                    try {
                        net.kyori.adventure.text.Component selectedComp = Triton.get().getMessagesConfig().getMessageComponent("other.currently-selected");
                        String selectedStr = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(selectedComp);
                        if (selectedStr.contains("Unknown message")) {
                            selectedComp = Triton.get().getMessagesConfig().getMessageComponent("other.selected");
                        }
                        com.rexcantor64.triton.api.language.TranslationResult<net.kyori.adventure.text.Component> selectedTrans = Triton.get().getMessageParser().translateComponent(
                            selectedComp, lp, Triton.get().getConfig().getGuiSyntax()
                        );
                        net.kyori.adventure.text.Component finalSelected = selectedTrans.isChanged() ? selectedTrans.getResultRaw() : selectedComp;
                        if (finalSelected != null) {
                            selectedStr = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(finalSelected);
                        }
                        if (selectedStr.contains("Unknown message")) {
                            selectedStr = "Currently selected";
                        }
                        btnText = btnText + " §8- " + selectedStr;
                    } catch (Throwable ignored) {
                        btnText = btnText + " §8- §aCurrently selected";
                    }
                }
                
                String imageUrl = getFlagUrl(countryCode);
                if (imageUrl != null) {
                    builder.button(btnText, org.geysermc.cumulus.util.FormImage.Type.URL, imageUrl);
                } else {
                    builder.button(btnText);
                }
            }

            builder.validResultHandler((form, response) -> {
                int clickedIndex = response.clickedButtonId();
                if (clickedIndex >= 0 && clickedIndex < languages.size()) {
                    com.rexcantor64.triton.language.Language selected = languages.get(clickedIndex);
                    lp.runSync(() -> {
                        lp.setLang(selected);
                        lp.sendSuccessMessage(selected);
                    });
                }
            });

            org.geysermc.cumulus.form.SimpleForm simpleForm = builder.build();

            // Send via Geyser or Floodgate
            if (hasGeyser) {
                try {
                    org.geysermc.geyser.api.connection.GeyserConnection connection = org.geysermc.geyser.api.GeyserApi.api().connectionByUuid(uuid);
                    if (connection != null) {
                        connection.sendForm(simpleForm);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
            if (hasFloodgate) {
                try {
                    org.geysermc.floodgate.api.FloodgateApi.getInstance().sendForm(uuid, simpleForm);
                    return true;
                } catch (Throwable ignored) {}
            }

            return false;
        }

        private static String getCountryCode(com.rexcantor64.triton.api.language.Language lang) {
            String code = null;
            String name = lang.getName();
            if (name != null) {
                String[] parts = name.split("[_-]");
                String lastPart = parts[parts.length - 1];
                if (lastPart.length() == 2) {
                    code = lastPart;
                }
            }
            if (code == null && lang.getMinecraftCodes() != null && !lang.getMinecraftCodes().isEmpty()) {
                String firstCode = lang.getMinecraftCodes().get(0);
                if (firstCode != null) {
                    String[] parts = firstCode.split("[_-]");
                    String lastPart = parts[parts.length - 1];
                    if (lastPart.length() == 2) {
                        code = lastPart;
                    }
                }
            }
            if (code == null) {
                if (name != null && name.length() == 2) {
                    code = name;
                } else if (lang.getFlagCode() != null && lang.getFlagCode().length() == 2) {
                    code = lang.getFlagCode();
                } else if (name != null && name.length() > 2) {
                    code = name.substring(0, 2);
                } else {
                    code = "us";
                }
            }
            return code.toLowerCase(Locale.ROOT);
        }

        private static String getFlagUrl(String flagCode) {
            if (flagCode == null) return null;
            String code = flagCode.toLowerCase(Locale.ROOT);
            switch (code) {
                case "en":
                    code = "gb";
                    break;
                case "ja":
                    code = "jp";
                    break;
                case "zh":
                    code = "cn";
                    break;
                case "ko":
                    code = "kr";
                    break;
                case "el":
                    code = "gr";
                    break;
                case "he":
                    code = "il";
                    break;
                case "da":
                    code = "dk";
                    break;
                case "cs":
                    code = "cz";
                    break;
                case "uk":
                    code = "ua";
                    break;
                case "sv":
                    code = "se";
                    break;
                case "nb":
                case "no":
                    code = "no";
                    break;
            }
            return "https://flagcdn.com/w160/" + code + ".png";
        }

        private static String getFlagEmoji(String flagCode) {
            if (flagCode == null || flagCode.length() != 2) {
                return "";
            }
            String code = flagCode.toUpperCase(Locale.ROOT);
            switch (code) {
                case "EN":
                    code = "GB";
                    break;
                case "JA":
                    code = "JP";
                    break;
                case "ZH":
                    code = "CN";
                    break;
                case "KO":
                    code = "KR";
                    break;
                case "EL":
                    code = "GR";
                    break;
                case "HE":
                    code = "IL";
                    break;
                case "DA":
                    code = "DK";
                    break;
                case "CS":
                    code = "CZ";
                    break;
                case "UK":
                    code = "UA";
                    break;
                case "SV":
                    code = "SE";
                    break;
                case "NB":
                case "NO":
                    code = "NO";
                    break;
            }
            try {
                int cp1 = 0x1F1E6 + (code.charAt(0) - 'A');
                int cp2 = 0x1F1E6 + (code.charAt(1) - 'A');
                return new String(new int[]{cp1, cp2}, 0, 2) + " ";
            } catch (Throwable t) {
                return "";
            }
        }
    }
}
