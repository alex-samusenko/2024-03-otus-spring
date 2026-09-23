namespace BongoCat.Core;

public static class HandSprites
{
    public const string Idle = "hand/idle.png";
    public const string Raised = "hand/raised.png";
    public const string Press = "hand/press.png";

    public static string ResolvePress(string? key, Func<string, bool> exists)
    {
        if (!string.IsNullOrWhiteSpace(key))
        {
            var specific = $"hand/keys/{key}.png";
            if (exists(specific))
                return specific;

            var zone = $"hand/zones/{KeyboardLayout.ZoneOf(key)}.png";
            if (exists(zone))
                return zone;
        }

        return Press;
    }
}
