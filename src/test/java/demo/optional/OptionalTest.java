package demo.optional;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
public class OptionalTest {

    @Test(expected = NoSuchElementException.class)
    public void whenCreateEmptyOptional_theNull() {
        Optional<User> emptyOpt = Optional.empty();
        emptyOpt.get();
    }

    @Test
    public void testOfNullable() {
        User user = new User("wuhan", "China", "430073");
        Optional<User> optional = Optional.ofNullable(user);

        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals(user.getAddress(), optional.get().getAddress());
    }

    @Test
    public void testOrElse() {
        User user = new User("wuhan", "China", "430073");
        User nullUser = null;
        User result = Optional.ofNullable(nullUser).orElse(user);
        Assert.assertEquals(user.getAddress(), result.getAddress());

        result = Optional.ofNullable(nullUser).orElseGet(() -> user);
        Assert.assertEquals(user.getAddress(), result.getAddress());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testOrElseThrow() {
        User result = (User) Optional.ofNullable(null).orElseThrow(() -> new IllegalArgumentException());

    }

    @Test
    public void testMap() {
        User user = new User("wuhan", "China", "430073");
        String address = Optional.ofNullable(user)
                .map(u -> u.getAddress()).orElse("hongshan");
        Assert.assertEquals("wuhan", address);
    }


    @Test
    public void testFilter() {
        User user = new User("wuhan", "China", "430073");
        Optional<User> result = Optional.ofNullable(user)
                .filter(u -> u.getAddress().equals("wuhan"));
        Assert.assertTrue(result.isPresent());
    }

    @Test
    public void testOptional() {
        OptionalUser user = new OptionalUser();
        String address = user.getAddress().orElse("hongshan");
        Assert.assertEquals("hongshan", address);
    }

    @Test
    public void testOrElseAndOrElseGet() {
        User user = new User("wuhan", "China", "430073");
        log.debug("Using orElse");
        User result = Optional.ofNullable(user).orElse(createNewUser());
        log.debug("Using orElseGet");
        result = Optional.ofNullable(user).orElseGet(this::createNewUser);
    }

    private User createNewUser() {
        log.debug("Creating new user");
        return new User("wuhan", "China", "430073");
    }
}
