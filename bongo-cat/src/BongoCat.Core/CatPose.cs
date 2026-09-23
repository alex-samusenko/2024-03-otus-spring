namespace BongoCat.Core;

public enum MouthFrame
{
    Closed = 0,
    Half = 1,
    Open = 2
}

public enum HandPose
{
    Idle,
    Press,
    Raised
}

public enum PointerButton
{
    None,
    Left,
    Right
}

public sealed record CatPose(
    int BangsFrame,
    MouthFrame Mouth,
    int EyeFrame,
    PointerButton Mouse,
    HandPose Hand,
    string? ActiveKey,
    string Zone,
    double KeyX,
    double KeyY);

public readonly record struct HandPlacement(double X, double Y);

public static class HandPlacementMath
{
    public static HandPlacement Resolve(
        CatPose pose,
        double canvasWidth,
        double canvasHeight,
        double keyboardX,
        double keyboardY,
        double keyboardW,
        double keyboardH,
        double restX,
        double restY,
        double raisedLift)
    {
        if (pose.Hand == HandPose.Idle)
            return new HandPlacement(restX * canvasWidth, restY * canvasHeight);

        var x = (keyboardX + pose.KeyX * keyboardW) * canvasWidth;
        var y = (keyboardY + pose.KeyY * keyboardH) * canvasHeight;
        if (pose.Hand == HandPose.Raised)
            y -= raisedLift * canvasHeight;

        return new HandPlacement(x, y);
    }
}
