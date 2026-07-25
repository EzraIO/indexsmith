package io.github.outakeshi.indexsmith.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JsonUtilTest {
    @Test
    public void serializesWithoutNullFields() throws Exception {
        User user = new User();
        user.name = "Ada";

        String json = JsonUtil.toJson(user);

        assertEquals("{\"name\":\"Ada\"}", json);
        assertFalse(json.contains("age"));
    }

    @Test
    public void ignoresUnknownProperties() throws Exception {
        User user = JsonUtil.fromJson("{\"name\":\"Ada\",\"unknown\":true}", User.class);
        assertEquals("Ada", user.name);
    }

    @Test
    public void deserializesGenericType() throws Exception {
        List<User> users = JsonUtil.fromJson("[{\"name\":\"Ada\"}]", new TypeReference<List<User>>() { });
        assertEquals(1, users.size());
        assertEquals("Ada", users.get(0).name);
    }

    public static class User {
        public String name;
        public Integer age;
    }
}
