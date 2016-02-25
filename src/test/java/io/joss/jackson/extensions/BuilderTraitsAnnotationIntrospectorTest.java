package io.joss.jackson.extensions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BuilderTraitsAnnotationIntrospectorTest
{

  public static class TestNonLombokClassWithBuilder
  {

    private TestNonLombokClassWithBuilder(int x)
    {

    }

    public String value;

    public String getValue()
    {
      return value;
    }

    public static class RefinedClassMadeByBuilder extends TestNonLombokClassWithBuilder
    {

      public RefinedClassMadeByBuilder(int x)
      {
        super(x);
      }

    }

    public static MyBuilder builder()
    {
      return new MyBuilder();
    }

    public static class MyBuilder
    {

      private RefinedClassMadeByBuilder obj = new RefinedClassMadeByBuilder(1);

      public MyBuilder value(String value)
      {
        obj.value = value;
        return this;
      }

      public RefinedClassMadeByBuilder build()
      {
        return obj;
      }

    }

  }

  @Test
  public void test() throws Exception
  {
    ObjectMapper mapper = new ObjectMapper();
    BuilderTraitsAnnotationIntrospector.install(mapper);
    TestNonLombokClassWithBuilder test0 = mapper.readValue("{ \"value\": \"Hello, World!\" }", TestNonLombokClassWithBuilder.class);
    assertEquals("Hello, World!", test0.value);
  }

}
