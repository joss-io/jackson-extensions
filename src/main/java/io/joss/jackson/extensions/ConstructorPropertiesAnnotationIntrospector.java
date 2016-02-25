package io.joss.jackson.extensions;

import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

/**
 * Allows deserialization into value based objects by using @ConstructorProperties on a constructor, which lombok provides.
 *
 * If there are multiple constructors avalable, you must annotate one with @JsonCreator to let it know which to use.
 *
 * Note that jackson 2.7 adds some rudimentary @ConstructorProperties support, but doesn't handle multiple constrcutors annotated with it.
 * This provides more fine grained support, allowing mapping of internal field names to external property ones.
 *
 * @author Theo Zourzouvillys
 *
 */

public class ConstructorPropertiesAnnotationIntrospector extends NopAnnotationIntrospector
{

  private static final long serialVersionUID = 1L;

  private static final ConstructorPropertiesAnnotationIntrospector INSTANCE = new ConstructorPropertiesAnnotationIntrospector();

  /**
   * finds the cached information about this class, to avoud multiple looks for each paramter.
   */

  private static boolean hasSelectedConstructor(Class<?> klass)
  {
    for (Constructor<?> ctor : klass.getConstructors())
    {
      if (ctor.getAnnotation(JsonCreator.class) != null)
      {
        return true;
      }
    }
    return false;
  }

  private static Constructor<?> info(Class<?> klass)
  {

    Constructor<?> found = null;

    for (Constructor<?> ctor : klass.getConstructors())
    {

      if (ctor.getAnnotation(JsonIgnore.class) != null)
      {
        continue;
      }

      ConstructorProperties props = ctor.getAnnotation(ConstructorProperties.class);

      if (props == null)
      {
        continue;
      }

      if (ctor.getAnnotation(JsonCreator.class) != null)
      {
        // short circuut.
        return ctor;
      }

      if (found != null)
      {
        throw new IllegalArgumentException(String.format("%s has multiple @ConstructorProperies ctors, select one with @JsonCreator", klass.getName()));
      }

      found = ctor;

    }

    return found;

  }

  /**
   * Method for finding implicit name for a property that given annotated member (field, method, creator parameter) may represent. This is
   * different from explicit, annotation-based property name, in that it is "weak" and does not either proof that a property exists (for
   * example, if visibility is not high enough), or override explicit names. In practice this method is used to introspect optional names
   * for creator parameters (which may or may not be available and can not be detected by standard databind); or to provide alternate name
   * mangling for fields, getters and/or setters.
   * 
   * @since 2.4
   */

  @Override
  public String findImplicitPropertyName(AnnotatedMember member)
  {

    if (member instanceof AnnotatedParameter)
    {

      AnnotatedParameter param = ((AnnotatedParameter) member);
      AnnotatedWithParams owner = param.getOwner();

      if (owner instanceof AnnotatedConstructor)
      {

        AnnotatedConstructor pwner = (AnnotatedConstructor) param.getOwner();

        Constructor<?> selected = info(pwner.getDeclaringClass());

        if (selected.equals(pwner.getAnnotated()))
        {

          if (param.getOwner().hasAnnotation(ConstructorProperties.class))
          {
            final String name = param.getOwner().getAnnotation(ConstructorProperties.class).value()[param.getIndex()];
            String pname = getParameterName(param, name);
            return pname;

          }

        }

      }

    }

    return super.findImplicitPropertyName(member);

  }

  /**
   * Given a construtctor parameter value, find the field and returns the JsonProperty paramter for it if one exists, otherwise returns the
   * name itself.
   * 
   * @param param
   * @param cpName
   * @return
   */

  private String getParameterName(AnnotatedParameter param, String cpName)
  {

    for (Field field : param.getOwner().getDeclaringClass().getDeclaredFields())
    {

      if (field.getName().equals(cpName))
      {

        final JsonProperty annotation = field.getAnnotation(JsonProperty.class);

        if (annotation == null)
        {
          return cpName;
        }
        else if (annotation.value() == null || "".equals(annotation.value()))
        {
          return field.getName();
        }
        else
        {
          return annotation.value();
        }

      }

    }

    return cpName;
  }

  /**
   * returns true if this constrcutor is eligible as a creator.
   */

  @Override
  public boolean hasCreatorAnnotation(final Annotated a)
  {

    if (!(a instanceof AnnotatedConstructor))
    {
      return false;
    }

    final AnnotatedConstructor ac = (AnnotatedConstructor) a;

    if (ac.hasAnnotation(JsonIgnore.class))
    {
      return false;
    }

    boolean requireSpecific = hasSelectedConstructor(ac.getRawType());

    if (requireSpecific)
    {
      Constructor<?> selected = info(ac.getRawType());
      return selected.equals(ac.getAnnotated());
    }

    boolean hasCreator = ac.getAnnotation(ConstructorProperties.class) != null;

    return hasCreator;

  }

  /**
   * Installs this annotation instroecpt on the given mapper.
   *
   * @param mapper
   */

  public static void install(final ObjectMapper mapper)
  {
    mapper.setAnnotationIntrospector(new AnnotationIntrospectorPair(
        INSTANCE, mapper.getDeserializationConfig().getAnnotationIntrospector()));
  }

}