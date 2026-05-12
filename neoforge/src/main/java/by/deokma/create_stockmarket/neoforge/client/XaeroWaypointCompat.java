package by.deokma.create_stockmarket.neoforge.client;

import by.deokma.create_stockmarket.shop.ShopEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class XaeroWaypointCompat {

    private static final String MOD_MINIMAP = "xaerominimap";
    private static final String MOD_WORLDMAP = "xaeroworldmap";

    private XaeroWaypointCompat() {}

    public static boolean isPresent() {
        ModList modList = ModList.get();
        return modList.isLoaded(MOD_MINIMAP) || modList.isLoaded(MOD_WORLDMAP);
    }

    public static boolean addShopWaypoint(ShopEntry entry) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (openXaeroAddWaypointScreen(entry)) {
            return true;
        }

        mc.player.displayClientMessage(Component.literal("Can't open Xaero waypoint screen"), true);
        return false;
    }

    private static String buildShareCode(ShopEntry entry) {
        int x = entry.pos().getX();
        int y = entry.pos().getY();
        int z = entry.pos().getZ();

        String name = sanitizeWaypointName(entry.ownerName() + " Shop");
        String marker = "S";
        int color = 10;
        boolean useYaw = false;
        int yaw = 0;
        String dimension = toXaeroDimension(entry.dimensionId());

        return "xaero-waypoint:"
                + name + ":"
                + marker + ":"
                + x + ":"
                + y + ":"
                + z + ":"
                + color + ":"
                + useYaw + ":"
                + yaw + ":"
                + dimension;
    }

    private static String sanitizeWaypointName(String raw) {
        String cleaned = raw.replace(":", " ").trim();
        if (cleaned.isEmpty()) cleaned = "Shop";
        if (cleaned.length() > 30) cleaned = cleaned.substring(0, 30);
        return cleaned;
    }

    private static String toXaeroDimension(String dimensionId) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        if (id == null) return "Internal-overworld-waypoints";
        if (id.equals(Level.OVERWORLD.location())) return "Internal-overworld-waypoints";
        if (id.equals(Level.NETHER.location())) return "Internal-the-nether-waypoints";
        if (id.equals(Level.END.location())) return "Internal-the-end-waypoints";
        return "Internal-overworld-waypoints";
    }

    private static boolean tryDirectXaeroInsert(ShopEntry entry) {
        try {
            Object waypointSet = resolveCurrentWaypointSet();
            if (waypointSet == null) return false;

            Object waypoint = buildWaypoint(waypointSet.getClass().getClassLoader(), entry);
            if (waypoint == null) return false;

            if (invokeAddMethod(waypointSet, waypoint)) return true;

            Method getList = waypointSet.getClass().getMethod("getList");
            Object listObj = getList.invoke(waypointSet);
            if (listObj instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Object> mutable = (List<Object>) list;
                mutable.add(waypoint);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean openXaeroAddWaypointScreen(ShopEntry entry) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Object waypoint = buildWaypoint(XaeroWaypointCompat.class.getClassLoader(), entry);
            if (waypoint == null) return false;
            Screen current = mc.screen;

            List<String> screenClasses = new ArrayList<>();
            screenClasses.add("xaero.hud.minimap.gui.GuiAddWaypoint");
            screenClasses.add("xaero.common.gui.GuiAddWaypoint");
            screenClasses.add("xaero.hud.minimap.gui.GuiWaypoint");
            screenClasses.add("xaero.common.gui.GuiWaypoint");
            screenClasses.addAll(discoverXaeroWaypointScreens());

            for (String className : screenClasses) {
                Screen built = tryCreateScreen(className, current, waypoint, entry);
                if (built != null) {
                    mc.setScreen(built);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static Screen tryCreateScreen(String className, Screen parent, Object waypoint, ShopEntry entry) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!Screen.class.isAssignableFrom(clazz)) return null;
            Constructor<?>[] ctors = clazz.getDeclaredConstructors();
            for (Constructor<?> ctor : ctors) {
                Object[] args = matchConstructorArgs(ctor.getParameterTypes(), parent, waypoint, entry);
                if (args == null) continue;
                ctor.setAccessible(true);
                Object screen = ctor.newInstance(args);
                if (screen instanceof Screen s) return s;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static List<String> discoverXaeroWaypointScreens() {
        List<String> out = new ArrayList<>();
        try {
            Class<?> anchor = Class.forName("xaero.hud.minimap.BuiltInHudModules");
            URI location = anchor.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Path.of(location);
            File file = path.toFile();
            if (!file.isFile()) return out;

            try (ZipFile zip = new ZipFile(file)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    String name = e.getName();
                    if (!name.endsWith(".class")) continue;
                    String low = name.toLowerCase(Locale.ROOT);
                    if (!low.contains("waypoint")) continue;
                    if (!(low.contains("gui") || low.contains("screen"))) continue;
                    if (!low.startsWith("xaero/")) continue;
                    String cls = name.substring(0, name.length() - 6).replace('/', '.');
                    out.add(cls);
                }
            }
            out.sort(Comparator.comparingInt(XaeroWaypointCompat::screenPriority));
        } catch (Exception ignored) {
        }
        return out;
    }

    private static int screenPriority(String className) {
        String s = className.toLowerCase(Locale.ROOT);
        if (s.contains("add") && s.contains("waypoint")) return 0;
        if (s.contains("new") && s.contains("waypoint")) return 1;
        if (s.contains("edit") && s.contains("waypoint")) return 2;
        return 10;
    }

    private static Object[] matchConstructorArgs(Class<?>[] params, Screen parent, Object waypoint, ShopEntry entry) {
        Object[] args = new Object[params.length];
        int intCounter = 0;
        for (int i = 0; i < params.length; i++) {
            Class<?> p = params[i];
            if (Screen.class.isAssignableFrom(p)) {
                args[i] = parent;
            } else if (p.isInstance(waypoint)) {
                args[i] = waypoint;
            } else if (p == String.class) {
                args[i] = "";
            } else if (p == int.class || p == Integer.class) {
                args[i] = switch (intCounter++) {
                    case 0 -> entry.pos().getX();
                    case 1 -> entry.pos().getY();
                    case 2 -> entry.pos().getZ();
                    default -> 0;
                };
            } else if (p == boolean.class || p == Boolean.class) {
                args[i] = false;
            } else if (p == float.class || p == Float.class) {
                args[i] = 0f;
            } else if (p == double.class || p == Double.class) {
                args[i] = 0d;
            } else if (p == long.class || p == Long.class) {
                args[i] = 0L;
            } else {
                return null;
            }
        }
        return args;
    }

    private static Object resolveCurrentWaypointSet() {
        try {
            Class<?> modulesClass = Class.forName("xaero.hud.minimap.BuiltInHudModules");
            Object minimapModule = modulesClass.getField("MINIMAP").get(null);
            Object session = minimapModule.getClass().getMethod("getCurrentSession").invoke(minimapModule);
            if (session == null) return null;
            Object worldManager = session.getClass().getMethod("getWorldManager").invoke(session);
            if (worldManager == null) return null;
            Object minimapWorld = worldManager.getClass().getMethod("getCurrentWorld").invoke(worldManager);
            if (minimapWorld == null) return null;
            return minimapWorld.getClass().getMethod("getCurrentWaypointSet").invoke(minimapWorld);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object buildWaypoint(ClassLoader loader, ShopEntry entry) {
        try {
            Class<?> waypointClass = Class.forName("xaero.hud.minimap.waypoint.Waypoint", true, loader);
            String shareCode = buildShareCode(entry);

            for (Method method : waypointClass.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) continue;
                if (!waypointClass.isAssignableFrom(method.getReturnType())) continue;
                if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != String.class) continue;
                method.setAccessible(true);
                Object parsed = method.invoke(null, shareCode);
                if (parsed != null) return parsed;
            }

            for (Constructor<?> ctor : waypointClass.getDeclaredConstructors()) {
                Object waypoint = tryConstructor(ctor, entry);
                if (waypoint != null) return waypoint;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Object tryConstructor(Constructor<?> ctor, ShopEntry entry) {
        try {
            Class<?>[] params = ctor.getParameterTypes();
            Object[] args = new Object[params.length];
            int intCounter = 0;
            int stringCounter = 0;
            for (int i = 0; i < params.length; i++) {
                Class<?> p = params[i];
                if (p == int.class || p == Integer.class) {
                    args[i] = switch (intCounter++) {
                        case 0 -> entry.pos().getX();
                        case 1 -> entry.pos().getY();
                        case 2 -> entry.pos().getZ();
                        case 3 -> 1;
                        default -> 0;
                    };
                } else if (p == String.class) {
                    args[i] = switch (stringCounter++) {
                        case 0 -> sanitizeWaypointName(entry.ownerName() + " Shop");
                        case 1 -> "S";
                        default -> "";
                    };
                } else if (p == boolean.class || p == Boolean.class) {
                    args[i] = false;
                } else if (p == float.class || p == Float.class) {
                    args[i] = 0f;
                } else if (p == double.class || p == Double.class) {
                    args[i] = 0d;
                } else if (p == long.class || p == Long.class) {
                    args[i] = 0L;
                } else {
                    return null;
                }
            }
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean invokeAddMethod(Object waypointSet, Object waypoint) {
        String[] methods = {"add", "addWaypoint", "addWP", "insertWaypoint"};
        for (String name : methods) {
            try {
                Method m = waypointSet.getClass().getMethod(name, waypoint.getClass());
                m.invoke(waypointSet, waypoint);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
