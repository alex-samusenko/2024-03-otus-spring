namespace BongoCat.Core;

public readonly record struct KeyAnchor(double X, double Y, string Zone);

public static class KeyboardLayout
{
    private const double Columns = 24;

    private static readonly Dictionary<string, KeyAnchor> Map = Build();

    public static IReadOnlyCollection<string> Keys => Map.Keys;

    public static bool TryGet(string? key, out KeyAnchor anchor)
    {
        if (key is not null && Map.TryGetValue(key, out anchor))
            return true;

        anchor = new KeyAnchor(0.36, 0.56, "center");
        return false;
    }

    public static string ZoneOf(string? key)
    {
        TryGet(key, out var anchor);
        return anchor.Zone;
    }

    private static Dictionary<string, KeyAnchor> Build()
    {
        var map = new Dictionary<string, KeyAnchor>(StringComparer.Ordinal);

        void Add(double unit, double row, string zone, params string[] names)
        {
            var anchor = new KeyAnchor((unit + 0.5) / Columns, RowY(row), zone);
            foreach (var name in names)
                map[name] = anchor;
        }

        string Side(double unit) => unit <= 4 ? "left" : unit >= 10 ? "right" : "center";

        void Key(double unit, double row, params string[] names) => Add(unit, row, Side(unit), names);

        Key(0, 0, "Escape");
        for (var i = 1; i <= 12; i++)
            Key(i, 0, "F" + i);
        Key(13.4, 0, "PrintScreen", "Snapshot");
        Key(14.6, 0, "Scroll");
        Key(15.8, 0, "Pause");
        for (var i = 13; i <= 24; i++)
            Key(13.2 + (i - 13) * 0.22, 0, "F" + i);

        Key(0, 1, "Oem3", "OemTilde");
        for (var i = 1; i <= 9; i++)
            Key(i, 1, "D" + i);
        Key(10, 1, "D0");
        Key(11, 1, "OemMinus");
        Key(12, 1, "OemPlus");
        Key(13.6, 1, "Back");

        Key(0.3, 2, "Tab");
        Key(1.4, 2, "Q");
        Key(2.5, 2, "W");
        Key(3.6, 2, "E");
        Key(4.7, 2, "R");
        Key(5.8, 2, "T");
        Key(6.9, 2, "Y");
        Key(8.0, 2, "U");
        Key(9.1, 2, "I");
        Key(10.2, 2, "O");
        Key(11.3, 2, "P");
        Key(12.4, 2, "Oem4", "OemOpenBrackets");
        Key(13.5, 2, "Oem6", "OemCloseBrackets");
        Key(14.7, 2, "Oem5", "OemPipe");

        Key(0.4, 3, "Capital", "CapsLock");
        Key(1.6, 3, "A");
        Key(2.7, 3, "S");
        Key(3.8, 3, "D");
        Key(4.9, 3, "F");
        Key(6.0, 3, "G");
        Key(7.1, 3, "H");
        Key(8.2, 3, "J");
        Key(9.3, 3, "K");
        Key(10.4, 3, "L");
        Key(11.5, 3, "Oem1", "OemSemicolon");
        Key(12.6, 3, "Oem7", "OemQuotes");
        Key(14.2, 3, "Enter", "Return");

        Key(0.0, 4, "Oem102", "OemBackslash");
        Key(0.5, 4, "LeftShift");
        Key(1.9, 4, "Z");
        Key(3.0, 4, "X");
        Key(4.1, 4, "C");
        Key(5.2, 4, "V");
        Key(6.3, 4, "B");
        Key(7.4, 4, "N");
        Key(8.5, 4, "M");
        Key(9.6, 4, "OemComma");
        Key(10.7, 4, "OemPeriod");
        Key(11.8, 4, "Oem2", "OemQuestion");
        Key(13.6, 4, "RightShift");

        Key(0.2, 5, "LeftCtrl");
        Key(1.6, 5, "LWin");
        Key(3.0, 5, "LeftAlt");
        Add(6.4, 5, "space", "Space");
        Key(10.2, 5, "RightAlt");
        Key(11.5, 5, "RWin");
        Key(12.6, 5, "Apps");
        Key(14.0, 5, "RightCtrl");

        Key(16.2, 1, "Insert");
        Key(17.3, 1, "Home");
        Key(18.4, 1, "PageUp", "Prior");
        Key(16.2, 2, "Delete");
        Key(17.3, 2, "End");
        Key(18.4, 2, "PageDown", "Next");
        Key(17.3, 4, "Up");
        Key(16.2, 5, "Left");
        Key(17.3, 5, "Down");
        Key(18.4, 5, "Right");

        Key(20.0, 0, "NumLock");
        Key(21.1, 0, "Divide");
        Key(22.2, 0, "Multiply");
        Key(23.3, 0, "Subtract");
        Key(20.0, 1, "NumPad7");
        Key(21.1, 1, "NumPad8");
        Key(22.2, 1, "NumPad9");
        Key(23.3, 1.5, "Add");
        Key(20.0, 2, "NumPad4");
        Key(21.1, 2, "NumPad5");
        Key(22.2, 2, "NumPad6");
        Key(20.0, 3, "NumPad1");
        Key(21.1, 3, "NumPad2");
        Key(22.2, 3, "NumPad3");
        Key(20.55, 4.2, "NumPad0");
        Key(22.2, 4.2, "Decimal", "Separator");

        return map;
    }

    private static double RowY(double row) => 0.04 + row * 0.155;
}
