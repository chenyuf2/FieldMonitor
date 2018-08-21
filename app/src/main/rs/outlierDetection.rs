#pragma version(1)

// Package name
#pragma rs java_package_name(daslab.com.fieldmonitoringv2)

// Relaxed floating point, not as accurate, but faster
#pragma rs_fp_relaxed

// Uncomment to debug
//#include "rs_debug.rsh"
// To debug us rsDebug("message", value);

// Hue sum
static int hueTotalSum = 0;

// Variance sum
static uint32_t varianceTotalSum = 0;

// Number of pixels in the image
static int numberOfPixels = 0;

// Hue value not normalized
static float imageHueAverage = 0;

// Variance value not normalized
static float imageHueVariance = 0;

// Hue Sum for the cell
static int hueSumCell = 0;

// The bitmap bitmapImage
rs_allocation bitmapImage;

// The 40x30 essentially 2d array
rs_allocation regionsOfInterest;

// initilizes all the values for regionsOfInterest to 0
void initROI(){
    for(int i = 0; i < 1200; i++){
        rsSetElementAt_int(regionsOfInterest, 0, i);
    }
}


void __attribute__((kernel)) findAreasOfInterest(uchar4 in, uint32_t x, uint32_t y){

    // If it goes beyond these markers, it is larger than we will calculate
    if(x > 3900 || y > 2900){
        return;
    }

    // Gets the first element of the row and column
    if( (x % 100 == 0) && (y % 100 == 0)){
        // Sets the hue sum cell to 0
        float hueSumCell = 0;
        for(int i = 0; i < 100; i++){
            for(int j = 0; j < 100; j++){
                // Gets the neighbor at i,j for a 100x100 grid from the starting x and y
                uchar4 neighbor = rsGetElementAt_uchar4(bitmapImage, x+i, y+j);

                // Starts the hue calculation
                float hue = 0.0f;

                float redPrime = native_divide(neighbor.r, 255.0f);
                float greenPrime = native_divide(neighbor.g, 255.0f);
                float bluePrime = native_divide(neighbor.b, 255.0f);

                float maxColor = fmax(redPrime, fmax(greenPrime, bluePrime));
                float minColor = fmin(redPrime, fmin(greenPrime, bluePrime));

                float colorDelta = maxColor - minColor;

                if(colorDelta > 0.0f){
                    if(redPrime == maxColor){
                        hue = 60 * (fmod((native_divide((greenPrime - bluePrime), colorDelta)),6));
                    }
                    else if(greenPrime == maxColor){
                        hue = 60 * ((native_divide((bluePrime - redPrime), colorDelta)) + 2);
                    }
                    else if (bluePrime == maxColor){
                        hue = 60 * ((native_divide((redPrime - greenPrime),colorDelta)) + 4);
                    }
                }

                if(hue < 0.0f){
                    hue += 360.0f;
                }
                // Ends the hue calculation
                hueSumCell += hue;
            }
        }
        // Cell average
        float cellHueAverage = hueSumCell/10000;

        // See if the cell hue average is less than the entire image hue + the variance
        if(cellHueAverage < (hueTotalSum + (varianceTotalSum/360))/12000000){
            // If true, set the element at x/100,y/100 to true (1)
            rsSetElementAt_int(regionsOfInterest, 1, x/100, y/100);
        }
    }

}

// Takes the rgb value of a pixel and computes the hue
void __attribute__((kernel)) addHueChannel(uchar4 in){
    float hue = 0.0f;

    // Start Compute Hue
    float redPrime = native_divide(in.r, 255.0f);
    float greenPrime = native_divide(in.g, 255.0f);
    float bluePrime = native_divide(in.b, 255.0f);

    float maxColor = fmax(redPrime, fmax(greenPrime, bluePrime));

    float minColor = fmin(redPrime, fmin(greenPrime, bluePrime));

    float colorDelta = maxColor - minColor;
    if(colorDelta > 0.0f){
        if(redPrime == maxColor){
            hue = 60 * (fmod((native_divide((greenPrime - bluePrime), colorDelta)),6));
        }
        else if(greenPrime == maxColor){
            hue = 60 * ((native_divide((bluePrime - redPrime), colorDelta)) + 2);
        }
        else if (bluePrime == maxColor){
            hue = 60 * ((native_divide((redPrime - greenPrime),colorDelta)) + 4);
        }
    }
    if(hue < 0.0f){
        hue += 360.0f;
    }
    // End Compute Hue

    // Add Hue to the totalsum
    rsAtomicAdd(&hueTotalSum, hue);

    // Increment the numberOfPixels
    rsAtomicInc(&numberOfPixels);


}

// Takes the rgb value of a pixel and computes the standard deviation squared
// Adds the standard deviation squared to varianceTotalSum
void __attribute__((kernel)) calculateVariance(uchar4 in){
    float hue = 0.0f;

    // Start Hue Calculation
    float redPrime = native_divide(in.r, 255.0f);
    float greenPrime = native_divide(in.g, 255.0f);
    float bluePrime = native_divide(in.b, 255.0f);

    float maxColor = fmax(redPrime, fmax(greenPrime, bluePrime));

    float minColor = fmin(redPrime, fmin(greenPrime, bluePrime));

    float colorDelta = maxColor - minColor;
    if(colorDelta > 0.0f){
        if(redPrime == maxColor){
            hue = 60 * (fmod((native_divide((greenPrime - bluePrime), colorDelta)),6));
        }
        else if(greenPrime == maxColor){
            hue = 60 * ((native_divide((bluePrime - redPrime), colorDelta)) + 2);
        }
        else if (bluePrime == maxColor){
            hue = 60 * ((native_divide((redPrime - greenPrime),colorDelta)) + 4);
        }
    }
    if(hue < 0.0f){
        hue += 360.0f;
    }
    // End Hue Calculation

    // Compute Standard Deviation Squared
    float variance = (hue - imageHueAverage) * (hue - imageHueAverage);

    // Add variance to varianceTotalSum
    rsAtomicAdd(&varianceTotalSum, variance);
}

// Returns the average hue of the entire image
float __attribute__((kernel)) getAverageImageHue(){
    imageHueAverage += native_divide((float)hueTotalSum,(float)numberOfPixels);
    return imageHueAverage;
}


// Returns the variance of the entire image
float __attribute__((kernel)) getVarianceImageHue(){
    imageHueVariance += native_divide((float)varianceTotalSum,(float)numberOfPixels);
    return imageHueVariance;
}

// Resets hue sum
void resetHueSum(){
    hueTotalSum = 0;
}

// Reset hue sum and number of pixels
void resetCounters(){
    hueTotalSum = 0;
    numberOfPixels = 0;
}