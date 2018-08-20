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

//static int regionsOfInterest[30][40];

static int pixCounter = 0;
static int hueSumCell = 0;

rs_allocation input;

rs_allocation regionsOfInterest;

void initROI(){
    for(int i = 0; i < 1200; i++){
        rsSetElementAt_int(regionsOfInterest, 0, i);
    }
}

void __attribute__((kernel)) findAreasOfInterest(uchar4 in, uint32_t x, uint32_t y){

    if(x > 3900 || y > 2900){
        //rsDebug("Will go out of bounds", "returning");
        return;// matrix;
    }
    if( (x % 100 == 0) && (y % 100 == 0)){
        //rsAtomicSub(&hueSumCell, hueSumCell);
        float hueSumCell = 0;
        //rsDebug("hueSumCell", hueSumCell);
        for(int i = 0; i < 100; i++){
            for(int j = 0; j < 100; j++){
                uchar4 neighbor = rsGetElementAt_uchar4(input, x+i, y+j);

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
                hueSumCell += hue;
                //rsAtomicAdd(&hueSumCell, hue);

            }
        }
        rsAtomicInc(&pixCounter);
        float cellHueAverage = hueSumCell/10000;
        if(cellHueAverage < (hueTotalSum + (varianceTotalSum/360))/12000000){
            rsSetElementAt_int(regionsOfInterest, 1, x/100, y/100);
        }
    }

}

int __attribute__((kernel)) returnPixCount(){
    return pixCounter;
}

int __attribute__((kernel)) returnHueSum(){
    return hueSumCell;
}

int __attribute__((kernel)) returnHuePlusVariance(){
    return (hueTotalSum + varianceTotalSum);
}

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

void divideVariance(){
    imageHueVariance = native_divide(imageHueVariance,129600.0f);
    //rsDebug("imageHueVar", imageHueVariance);
}

void divideAverage(){
    imageHueAverage = native_divide(imageHueAverage, 360.0f);
    //rsDebug("imageHueAvg", imageHueAverage);
}

// Returns the average hue of the entire image
float __attribute__((kernel)) getAverageImageHue(){
    imageHueAverage += native_divide((float)hueTotalSum,(float)numberOfPixels);
    //rsDebug("imageHueAvg", imageHueAverage);
    return imageHueAverage;
}


// Returns the variance of the entire image
float __attribute__((kernel)) getVarianceImageHue(){
    imageHueVariance += native_divide((float)varianceTotalSum,(float)numberOfPixels);
    //rsDebug("imageHueVar", imageHueVariance);
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