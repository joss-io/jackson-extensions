package io.joss.jackson.extensions;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

/**
 * Provides support for automatic construction of objects in Jackson using the builder pattern without needing to annotate the class.
 * 
 * If the class has a static no-args method named "builder" which returns another class that contains a method "build", then it will match.
 *
 * The builder will use an empty prefix for the setters, e.g "Builder.value(val)".
 *
 * @author theo
 *
 */
public class BuilderTraitsAnnotationIntrospector extends NopAnnotationIntrospector
{

  private static final long serialVersionUID = 1L;

  private static final String DEFAULT_BUILDER_CREATOR_METHOD_NAME = "builder";
  private static final String DEFAULT_BUILDER_BUILD_METHOD_NAME = "build";

  private Predicate<AnnotatedClass> onlyfor;

  public BuilderTraitsAnnotationIntrospector(Predicate<AnnotatedClass> onlyfor)
  {
    this.onlyfor = onlyfor;
  }

  /**
   * find the builder, and returns it.
   */

  public Class<?> findPOJOBuilder(AnnotatedClass ac)
  {

    if (!onlyfor.test(ac))
    {
      return super.findPOJOBuilder(ac);
    }

    for (AnnotatedMethod m : ac.getStaticMethods())
    {

      if (m.getName().equals(getBuilderCreatorName()) && m.isPublic() && m.getParameterCount() == 0)
      {

        // looks like this is a builder, let's make sure ...
        Class<?> builder = m.getRawReturnType();

        for (java.lang.reflect.Method bm : builder.getMethods())
        {

          if (!bm.getName().equals(getBuilderBuildName()))
          {
            continue;
          }
          else if (bm.getParameterCount() != 0)
          {
            continue;
          }
          else if (!ac.getRawType().isAssignableFrom(bm.getReturnType()))
          {
            continue;
          }

          // all the methd accesses are there.
          return builder;

        }

      }

    }

    return super.findPOJOBuilder(ac);

  }

  private String getBuilderCreatorName()
  {
    return DEFAULT_BUILDER_CREATOR_METHOD_NAME;
  }

  private String getBuilderBuildName()
  {
    return DEFAULT_BUILDER_BUILD_METHOD_NAME;
  }

  /**
   * returns a POJO builder value directly generated from the config.
   */

  public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac)
  {

    return new JsonPOJOBuilder.Value(new JsonPOJOBuilder() {

      @Override
      public Class<? extends Annotation> annotationType()
      {
        return JsonPOJOBuilder.class;
      }

      @Override
      public String buildMethodName()
      {
        return getBuilderBuildName();
      }

      @Override
      public String withPrefix()
      {
        return "";
      }

    });

  }

  /**
   * Installs builder pattern for the given mapper that uses a builder for all.
   */

  public static void install(final ObjectMapper mapper)
  {
    install(mapper, ac -> true);
  }

  /**
   * Enabled builder pattern support only for classes that match the given predicate (and match the builder requirements).
   */

  public static void install(final ObjectMapper mapper, Predicate<AnnotatedClass> onlyfor)
  {
    mapper.setAnnotationIntrospector(
        new AnnotationIntrospectorPair(
            mapper.getDeserializationConfig().getAnnotationIntrospector(),
            new BuilderTraitsAnnotationIntrospector(onlyfor)));
  }

}