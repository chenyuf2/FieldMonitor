#pragma version(1)
#pragma rs java_package_name(daslab.com.fieldmonitoringv2)
#pragma rs_fp_relaxed

#include "rs_debug.rsh"

// Use two global counters
static int hueTotalSum = 0;
static uint32_t varianceTotalSum = 0;
static int numberOfPixels = 0;
static float imageHueAverage = 0;
static float imageHueVariance = 0;

// Takes the rgb value of a pixel and computes the hue
// Adds the standard deviation squared to hueTotalSum
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
    imageHueAverage = native_divide(imageHueAverage, 360.0f);
    return imageHueAverage;
}


// Returns the variance of the entire image
float __attribute__((kernel)) getVarianceImageHue(){
    imageHueVariance += native_divide((float)varianceTotalSum,(float)numberOfPixels);
    imageHueVariance = native_divide(imageHueVariance,129600.0f);
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