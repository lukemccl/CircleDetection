# CircleDetection
A java program to detect circles within an image given with the command line, currently uses an implementation of Hough Transform

Outputs 6 files to the root /results directory consisting of the horizontal and vertical scans of the image, the combined sobel image, the edgepoints detected that are above the threshold, the hits for each point on the new image for every radius from edgepoints, and the original image with the greatest matching circle highlighted 

## Use 

Compile the program, call like any java program with arguments:

[File Path] [Num Drawn Circles (default 1)] [Sobel Threshold (default 150)] [Minimum radius circle (default 10)]

