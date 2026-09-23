namespace BongoCat.Core;

public interface IRandomSource
{
    int NextInt(int minInclusive, int maxExclusive);

    double NextDouble();
}

public sealed class SystemRandomSource : IRandomSource
{
    private readonly Random _random = new();

    public int NextInt(int minInclusive, int maxExclusive) => _random.Next(minInclusive, maxExclusive);

    public double NextDouble() => _random.NextDouble();
}
