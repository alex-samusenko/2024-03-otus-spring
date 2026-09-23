using System.IO;
using System.Windows.Media.Imaging;
using BongoCat.Core;

namespace BongoCat.App;

public sealed class AssetLibrary
{
    private readonly HashSet<string> _paths = new(StringComparer.OrdinalIgnoreCase);
    private readonly Dictionary<string, BitmapImage> _extraPress = new(StringComparer.OrdinalIgnoreCase);

    public BitmapImage? Body { get; private set; }
    public BitmapImage?[] Bangs { get; } = new BitmapImage?[3];
    public BitmapImage? MouthClosed { get; private set; }
    public BitmapImage? MouthHalf { get; private set; }
    public BitmapImage? MouthOpen { get; private set; }
    public BitmapImage? EyeOpen { get; private set; }
    public BitmapImage? EyeHalf { get; private set; }
    public BitmapImage? EyeClosed { get; private set; }
    public BitmapImage? MouseIdle { get; private set; }
    public BitmapImage? MouseLeft { get; private set; }
    public BitmapImage? MouseRight { get; private set; }
    public BitmapImage? HandIdle { get; private set; }
    public BitmapImage? HandRaised { get; private set; }
    public BitmapImage? HandPress { get; private set; }
    public double CanvasWidth { get; private set; } = 960;
    public double CanvasHeight { get; private set; } = 640;
    public int LoadedCount { get; private set; }

    public void Load(string root)
    {
        _paths.Clear();
        _extraPress.Clear();
        LoadedCount = 0;

        Body = Load(root, "body.png");
        Bangs[0] = Load(root, "bangs/1.png");
        Bangs[1] = Load(root, "bangs/2.png");
        Bangs[2] = Load(root, "bangs/3.png");
        MouthClosed = Load(root, "mouth/closed.png");
        MouthHalf = Load(root, "mouth/half.png");
        MouthOpen = Load(root, "mouth/open.png");
        EyeOpen = Load(root, "eyes/open.png");
        EyeHalf = Load(root, "eyes/half.png");
        EyeClosed = Load(root, "eyes/closed.png");
        MouseIdle = Load(root, "mouse/idle.png");
        MouseLeft = Load(root, "mouse/left.png");
        MouseRight = Load(root, "mouse/right.png");
        HandIdle = Load(root, HandSprites.Idle);
        HandRaised = Load(root, HandSprites.Raised);
        HandPress = Load(root, HandSprites.Press);
        LoadFolder(root, "hand/keys");
        LoadFolder(root, "hand/zones");

        if (Body is not null)
        {
            CanvasWidth = Math.Max(1, Body.Width);
            CanvasHeight = Math.Max(1, Body.Height);
        }
    }

    public BitmapImage? PressFor(string? key)
    {
        var relative = HandSprites.ResolvePress(key, Has);
        if (string.Equals(relative, HandSprites.Press, StringComparison.OrdinalIgnoreCase))
            return HandPress;

        return _extraPress.TryGetValue(relative, out var image) ? image : HandPress;
    }

    public bool Has(string relative) => _paths.Contains(Normalize(relative));

    private void LoadFolder(string root, string relativeFolder)
    {
        var directory = Path.Combine(root, relativeFolder.Replace('/', Path.DirectorySeparatorChar));
        if (!Directory.Exists(directory))
            return;

        foreach (var file in Directory.EnumerateFiles(directory, "*.png"))
        {
            var name = Path.GetFileName(file);
            var relative = Normalize(relativeFolder + "/" + name);
            var image = LoadAbsolute(file, relative);
            if (image is not null)
                _extraPress[relative] = image;
        }
    }

    private BitmapImage? Load(string root, string relative)
    {
        var path = Path.Combine(root, relative.Replace('/', Path.DirectorySeparatorChar));
        return File.Exists(path) ? LoadAbsolute(path, relative) : null;
    }

    private BitmapImage? LoadAbsolute(string path, string relative)
    {
        try
        {
            var image = new BitmapImage();
            image.BeginInit();
            image.CacheOption = BitmapCacheOption.OnLoad;
            image.CreateOptions = BitmapCreateOptions.IgnoreImageCache;
            image.UriSource = new Uri(path);
            image.EndInit();
            image.Freeze();
            _paths.Add(Normalize(relative));
            LoadedCount++;
            return image;
        }
        catch
        {
            return null;
        }
    }

    private static string Normalize(string relative) => relative.Replace('\\', '/');
}
