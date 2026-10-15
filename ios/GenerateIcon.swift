import AppKit
import ImageIO

let destination = CommandLine.arguments.dropFirst().first ?? "ios/Assets.xcassets/AppIcon.appiconset/AppIcon.png"
let c = CGContext(data: nil, width: 1024, height: 1024, bitsPerComponent: 8, bytesPerRow: 4096,
                  space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
c.setFillColor(NSColor(srgbRed: 0.04, green: 0.13, blue: 0.22, alpha: 1).cgColor)
c.fill(CGRect(x: 0, y: 0, width: 1024, height: 1024))
c.setFillColor(NSColor(srgbRed: 0.1, green: 0.4, blue: 0.5, alpha: 1).cgColor)
c.fillEllipse(in: CGRect(x: 105, y: 105, width: 814, height: 814))
let sword = CGMutablePath()
sword.move(to: CGPoint(x: 330, y: 370)); sword.addLine(to: CGPoint(x: 670, y: 745)); sword.addLine(to: CGPoint(x: 790, y: 805)); sword.addLine(to: CGPoint(x: 745, y: 670)); sword.addLine(to: CGPoint(x: 415, y: 300)); sword.closeSubpath()
c.setFillColor(NSColor(srgbRed: 0.87, green: 0.97, blue: 1, alpha: 1).cgColor); c.addPath(sword); c.fillPath()
c.setStrokeColor(NSColor(srgbRed: 1, green: 0.79, blue: 0.25, alpha: 1).cgColor); c.setLineWidth(55); c.setLineCap(.round)
c.move(to: CGPoint(x: 290, y: 440)); c.addLine(to: CGPoint(x: 470, y: 270)); c.strokePath()
c.setStrokeColor(NSColor(srgbRed: 0.64, green: 0.36, blue: 0.15, alpha: 1).cgColor); c.setLineWidth(65)
c.move(to: CGPoint(x: 350, y: 335)); c.addLine(to: CGPoint(x: 245, y: 220)); c.strokePath()
let output = CGImageDestinationCreateWithURL(URL(fileURLWithPath: destination) as CFURL, "public.png" as CFString, 1, nil)!
CGImageDestinationAddImage(output, c.makeImage()!, nil)
guard CGImageDestinationFinalize(output) else { fatalError("Could not write iOS app icon") }
