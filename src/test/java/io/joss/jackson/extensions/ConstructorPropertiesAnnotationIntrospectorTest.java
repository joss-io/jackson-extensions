package io.joss.jackson.extensions;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

public class ConstructorPropertiesAnnotationIntrospectorTest
{

  @Value
  private static class TestDate
  {
    private String moo;
  }

  @Value
  private static class AnotherTestDate
  {
    private String moo;
    private int count;
  }

  @Getter
  @RequiredArgsConstructor(onConstructor = @__(@JsonIgnore) )
  @AllArgsConstructor
  private static class YATD
  {
    private final String moo;
    private int count;
  }

  @RequiredArgsConstructor
  @AllArgsConstructor
  private static class BadTestDate
  {
    private final String moo;
    private int count;
  }

  @Value
  private static class AnnotatedTestDate
  {

    private final String moo;

    @JsonProperty("clk")
    private final String cluck;

  }

  private static ObjectMapper mapper;

  @BeforeClass
  public static void setup()
  {
    mapper = new ObjectMapper();
    ConstructorPropertiesAnnotationIntrospector.install(mapper);
  }

  @Test
  public void testSingleArgument() throws Exception
  {
    final TestDate test = mapper.readValue("{ \"moo\" : \"cows\" }", TestDate.class);
    assertEquals("cows", test.getMoo());
  }

  @Test
  public void testMultipleArguments() throws Exception
  {
    AnotherTestDate test = mapper.readValue("{\"moo\":\"cows\",\"count\":1}", AnotherTestDate.class);
    assertEquals("cows", test.getMoo());
    assertEquals(1, test.getCount());
  }

  @Test
  public void testMultipleAnnotatedConstructors() throws Exception
  {
    YATD test = mapper.readValue("{\"moo\":\"cows\",\"count\":1}", YATD.class);
    assertEquals("cows", test.getMoo());
    assertEquals(1, test.getCount());
  }

  @Test(expected = JsonProcessingException.class)
  public void testBadMultipleAnnotatedConstructors() throws Exception
  {
    mapper.readValue("{\"moo\":\"cows\",\"count\":1}", BadTestDate.class);
  }

  @Test
  public void testAnnotatedField() throws Exception
  {
    mapper.readValue("{\"moo\":\"cows\",\"clk\":\"cluck\"}", AnnotatedTestDate.class);
  }

}