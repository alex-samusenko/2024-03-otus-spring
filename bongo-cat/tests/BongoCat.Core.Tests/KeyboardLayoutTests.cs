using BongoCat.Core;

namespace BongoCat.Core.Tests;

public class KeyboardLayoutTests
{
    [Fact]
    public void Letters_digits_and_arrows_have_distinct_spots()
    {
        foreach (var letter in "QWERTYUIOPASDFGHJKLZXCVBNM")
            Assert.True(KeyboardLayout.TryGet(letter.ToString(), out _));

        for (var digit = 0; digit <= 9; digit++)
            Assert.True(KeyboardLayout.TryGet("D" + digit, out _));

        Assert.True(KeyboardLayout.TryGet("Left", out var left));
        Assert.True(KeyboardLayout.TryGet("Right", out var right));
        Assert.True(KeyboardLayout.TryGet("Space", out var space));
        Assert.True(KeyboardLayout.TryGet("NumPad5", out var numpad));
        Assert.NotEqual(left, right);
        Assert.Equal("space", space.Zone);
        Assert.Equal("right", numpad.Zone);
    }

    [Fact]
    public void Aliases_share_a_key()
    {
        Assert.True(KeyboardLayout.TryGet("Enter", out var enter));
        Assert.True(KeyboardLayout.TryGet("Return", out var ret));
        Assert.Equal(enter, ret);

        Assert.True(KeyboardLayout.TryGet("Oem4", out var bracket));
        Assert.True(KeyboardLayout.TryGet("OemOpenBrackets", out var open));
        Assert.Equal(bracket, open);
    }

    [Fact]
    public void Missing_key_falls_back_to_center()
    {
        Assert.False(KeyboardLayout.TryGet("VolumeUp", out var anchor));
        Assert.Equal("center", anchor.Zone);
        Assert.Equal("center", KeyboardLayout.ZoneOf("VolumeUp"));
    }

    [Fact]
    public void Press_sprite_prefers_a_key_then_a_zone()
    {
        Assert.Equal("hand/press.png", HandSprites.ResolvePress("A", _ => false));
        Assert.Equal(
            "hand/zones/left.png",
            HandSprites.ResolvePress("A", path => path == "hand/zones/left.png"));
        Assert.Equal(
            "hand/keys/A.png",
            HandSprites.ResolvePress("A", path => path is "hand/keys/A.png" or "hand/zones/left.png"));
        Assert.Equal("hand/press.png", HandSprites.ResolvePress(null, _ => true));
    }
}
