/*
 *  interface.h
 *  kinjext
 *
 *  Created by Craig Andera on 1/28/13.
 *  Copyright 2013 __MyCompanyName__. All rights reserved.
 *
 */

typedef struct KeyEvent {
	unsigned int keyCode;
	bool goingDown;
} KeyEvent;

enum KeyRequestCode {
	kInjectKeySendKey
};