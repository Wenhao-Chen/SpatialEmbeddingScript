### A simple example of running the script:
1. put 'SpatialScript.jar' and folder 'message_dictionary' in the same directory
2. determine the path of the input image(s) - could be the path of an image or the path to a folder containing multiple images
3. determine the path of the output folder where stego images will be stored
4. open terminal
5. use the 'cd' command to go to the directory where you put 'SpatialScript.jar'
6. assuming the input images are located in folder 'test_input' and you would like the output folder to be named 'stego_output', run command:

	`java -jar SpatialScript.jar message_dictionary test_input stego_output`
	
7. wait for the script to finish


### Embedding rates
by default, each stego app will create 5 stego images from 1 input image at rates: 5%, 10%, 15%, 20%, 25%. You can change this by adding additional parameters in the java command. For example:

	`java -jar SpatialScript.jar message_dictionary test_input stego_output 0.10 0.20 0.02`
	
will change the embedding rates to: 10% to 20% at 2% intervals (10%, 12%, 14%, ..., 20%)


### Validation
by default, the generated stego images are not validated. You can turn validation on by adding 'true' at the end of the command:

	`java -jar SpatialScript.jar message_dictionary test_input stego_output 0.10 0.20 0.02 true`
	
Beware that turning validation on will increase the runtime of the script.
