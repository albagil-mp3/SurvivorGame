from PIL import Image, ImageDraw

# Create simple square images for maze walls
def create_square_image(filename, color, size=40):
    img = Image.new('RGBA', (size, size), color)
    img.save(filename)
    print(f"Created {filename}")

# Gray walls for maze
create_square_image('src/resources/images/wall-01.png', (60, 60, 60, 255))
create_square_image('src/resources/images/wall-02.png', (80, 80, 80, 255))
create_square_image('src/resources/images/wall-03.png', (100, 100, 100, 255))
create_square_image('src/resources/images/wall-04.png', (70, 70, 90, 255))

print("\nWall images created successfully!")
print("Now update ProjectAssets.java to register these assets.")
