package squeek.asmhelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.launchwrapper.LaunchClassLoader;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class ObfHelper
{
	private static Boolean obfuscated = null;
	private static HashMap<String, String> classNameToObfClassNameCache = new HashMap<String, String>();
	private static HashMap<String, String> obfClassNameToClassNameCache = new HashMap<String, String>();

	/**
	 * Can be initialized by a core mod in {@link cpw.mods.fml.relauncher.IFMLLoadingPlugin#injectData} by 
	 * using the value of "runtimeDeobfuscationEnabled" to
	 * avoid the class loader lookup in isObfuscated.<br>
	 * <br>
	 * <b>Example:</b>
	 * <pre>
	 * public void injectData(Map<String, Object> data)
	 * {
	 *     ObfHelper.setObfuscated((Boolean) data.get("runtimeDeobfuscationEnabled"));
	 * }
	 * </pre>
	 */
	public static void setObfuscated(boolean obfuscated)
	{
		ObfHelper.obfuscated = obfuscated;
	}

	/**
	 * @return Whether or not the current environment contains obfuscated Minecraft code
	 */
	public static boolean isObfuscated()
	{
		if (obfuscated == null)
		{
			try
			{
				byte[] bytes = ((LaunchClassLoader) ObfHelper.class.getClassLoader()).getClassBytes("net.minecraft.world.World");
				ObfHelper.setObfuscated(bytes == null);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return obfuscated;
	}

	private static void cacheObfClassMapping(String obfClassName, String className)
	{
		obfClassNameToClassNameCache.put(obfClassName, className);
		classNameToObfClassNameCache.put(className, obfClassName);
	}

	/**
	 * Deobfuscates an obfuscated class name if {@link #isObfuscated()}.
	 */
	public static String toDeobfClassName(String obfClassName)
	{
		if (isObfuscated())
			return forceToDeobfClassName(obfClassName);
		else
			return obfClassName;
	}

	/**
	 * Deobfuscates an obfuscated class name regardless of {@link #isObfuscated()}.
	 */
	public static String forceToDeobfClassName(String obfClassName)
	{
		if (!obfClassNameToClassNameCache.containsKey(obfClassName))
		{
			String mapped = FMLDeobfuscatingRemapper.INSTANCE.map(obfClassName.replace('.', '/')).replace('/', '.');
			if (!mapped.equals(obfClassName))
				cacheObfClassMapping(mapped, obfClassName);
			else
				return obfClassName;
		}

		return obfClassNameToClassNameCache.get(obfClassName);
	}

	/**
	 * Obfuscates a deobfuscated class name if {@link #isObfuscated()}.
	 */
	public static String toObfClassName(String deobfClassName)
	{
		if (isObfuscated())
			return forceToObfClassName(deobfClassName);
		else
			return deobfClassName;
	}

	/**
	 * Obfuscates a deobfuscated class name regardless of {@link #isObfuscated()}.
	 */
	public static String forceToObfClassName(String deobfClassName)
	{
		if (!classNameToObfClassNameCache.containsKey(deobfClassName))
		{
			String unmapped = FMLDeobfuscatingRemapper.INSTANCE.unmap(deobfClassName.replace('.', '/')).replace('/', '.');
			if (!unmapped.equals(deobfClassName))
				cacheObfClassMapping(unmapped, deobfClassName);
			else
				return deobfClassName;
		}

		return classNameToObfClassNameCache.get(deobfClassName);
	}

	/**
	 * Converts a class name to an internal class name, obfuscating the class name if {@link #isObfuscated()}.
	 * @return internal/class/name
	 */
	public static String getInternalClassName(String className)
	{
		return toObfClassName(className).replace('.', '/');
	}

	/**
	 * Converts a class name to a descriptor, obfuscating the class name if {@link #isObfuscated()}.
	 * @return Linternal/class/name;
	 */
	public static String getDescriptor(String className)
	{
		return "L" + getInternalClassName(className) + ";";
	}

	/**
	 * Processes a descriptor, obfuscating class names if {@link #isObfuscated()}.
	 */
	public static String desc(String deobfDesc)
	{
		if (isObfuscated())
		{
			// for each internal name, replace with the obfuscated version
			Matcher classNameMatcher = Pattern.compile("L([^;]+);").matcher(deobfDesc);
			StringBuffer obfDescBuffer = new StringBuffer(deobfDesc.length());
			while (classNameMatcher.find())
			{
				classNameMatcher.appendReplacement(obfDescBuffer, getDescriptor(classNameMatcher.group(1).replace('/', '.')));
			}
			classNameMatcher.appendTail(obfDescBuffer);
			return obfDescBuffer.toString();
		}
		else
			return deobfDesc;
	}
}