package com.rayworks.droidweekly.repository.exception

import java.io.IOException

/***
 * An exception indicates the occurred error when parsing web content.
 */
class WebContentParsingException(msg: String) : IOException(msg)
