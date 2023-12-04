import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MakeHeader
{

    public static class VideoCommand {

        public class VideoHeaderSizes {
            private int srcWidth = 0, srcHeight = 0, srcLeft = 0, srcTop = 0, srcRight = 0, srcBottom = 0;
            private int destWidth = 0, destHeight = 0, reserved0 = 0, reserved1 = 0, reserved2 = 0;
            private boolean extResampling = false;
            private float resampTag = 1;
            
            public VideoHeaderSizes(byte[] buffer, boolean extendedResampling)
            {
                if (buffer == null || buffer.length < 48)
                    return;
                srcWidth	= bytesToInt(buffer,  0);
                srcHeight	= bytesToInt(buffer,  4);
                srcLeft		= bytesToInt(buffer,  8);
                srcTop		= bytesToInt(buffer, 12);
                srcRight	= bytesToInt(buffer, 16);
                srcBottom	= bytesToInt(buffer, 20);
                destWidth	= bytesToInt(buffer, 24);
                destHeight	= bytesToInt(buffer, 28);
                resampTag	= bytesToInt(buffer, 32);
                if (extendedResampling) {
                    extResampling = true;
                    resampTag /= 1000000;
                }
                reserved0	= bytesToInt(buffer, 36);
                reserved1	= bytesToInt(buffer, 40);
                reserved2	= bytesToInt(buffer, 44);
            }
            
            public VideoHeaderSizes(int srcWidthArg, int srcHeightArg, int destWidthArg, int destHeightArg)
            {
                srcWidth = srcRight = srcWidthArg;
                srcHeight = srcBottom = srcHeightArg;
                destWidth = destWidthArg;
                destHeight = destHeightArg;
            }
            
            public byte[] toByteArray()
            {
                byte[] headerSizesBytes = new byte[48];
                System.arraycopy(intToByteArray(srcWidth),	0, headerSizesBytes,  0, 4);
                System.arraycopy(intToByteArray(srcHeight), 0, headerSizesBytes,  4, 4);
                System.arraycopy(intToByteArray(srcLeft),	0, headerSizesBytes,  8, 4);
                System.arraycopy(intToByteArray(srcTop),	0, headerSizesBytes, 12, 4);
                System.arraycopy(intToByteArray(srcRight),	0, headerSizesBytes, 16, 4);
                System.arraycopy(intToByteArray(srcBottom),	0, headerSizesBytes, 20, 4);
                System.arraycopy(intToByteArray(destWidth), 0, headerSizesBytes, 24, 4);
                System.arraycopy(intToByteArray(destHeight),0, headerSizesBytes, 28, 4);
                int resTagInt = (int)resampTag;
                if (extResampling)
                    resTagInt = (int)(resampTag * 1000000);
                System.arraycopy(intToByteArray(resTagInt),	0, headerSizesBytes, 32, 4);
                System.arraycopy(intToByteArray(reserved0),	0, headerSizesBytes, 36, 4);
                System.arraycopy(intToByteArray(reserved1), 0, headerSizesBytes, 40, 4);
                System.arraycopy(intToByteArray(reserved2),0, headerSizesBytes, 44, 4);
                return headerSizesBytes;
            }
            
            public int getSrcWidth() { return srcWidth; }
            public int getSrcHeight() { return srcHeight; }
            public int getSrcLeft() { return srcLeft; }
            public int getSrcTop() { return srcTop; }
            public int getSrcRight() { return srcRight; }
            public int getSrcBottom() { return srcBottom; }
            
            public int getDestWidth() { return destWidth; }
            public int getDestHeight() { return destHeight; }
            public float getResampTag() { return resampTag; }
            public int getReserved0() { return reserved0; }
            public int getReserved1() { return reserved1; }
            public int getReserved2() { return reserved2; }
            
            @Override
            public String toString()
            {
                StringBuilder sb = new StringBuilder("Video Sizes { srcWidth: ").append(srcWidth).append(", srcHeight: ").append(srcHeight);
                sb.append(" {top: ").append(srcTop).append(", left: ").append(srcLeft).append(", right: ").append(srcRight).append(", bottom: ").append(srcBottom);
                sb.append("}, destWidth: ").append(destWidth).append(", destHeight: ").append(destHeight);
                sb.append(" {reserved0: ").append(reserved0).append(", resampTag: ").append(resampTag).append(", reserved1: ").append(reserved1).append(", reserved2: ").append(reserved2);
                sb.append("} }");
                return sb.toString();
            }
        }

        public class VideoHeaderLiveEvents {
            private int currentFlags = 0, changedFlags = 0;
            
            public VideoHeaderLiveEvents(byte[] buffer, int hOffset)
            {
                if (buffer == null || hOffset < 0 || buffer.length < hOffset+8)
                    return;
                
                currentFlags = bytesToInt(buffer, hOffset);
                changedFlags = bytesToInt(buffer, hOffset+4);
            }
            
            public VideoHeaderLiveEvents(int currentFlagsArg, int changedFlagsArg)
            {
                currentFlags = currentFlagsArg;
                changedFlags = changedFlagsArg;
            }
            
            public int getCurrentFlags() { return currentFlags; }
            public int getChangedFlags() { return changedFlags; }
        }
	
        public static final int HEADER_SIZES = 0x01;
        public static final int HEADER_LIVE_EVENTS = 0x02;
        public static final int HEADER_PLAYBACK_EVENTS = 0x04;
        public static final int HEADER_EXTENSION_NATIVE_DATA = 0x08;
        public static final int HEADER_EXTENSION_MOTION_EVENTS = 0x10;//arcus only
        public static final int HEADER_LOCATION_INFO = 0x20;
        public static final int HEADER_STREAM_INFO = 0x40;
        public static final int HEADER_ALL = 0x07;
        
        private String vId = null; // 16 bytes
        private byte[] vIdBytes = null; // 16 bytes
        private long timeStamp = 0; // 8 bytes
        private int frameCount = 0; // 4 bytes
        private int payloadSize = 0; // 4 bytes
        private short extHeaderSize = 0; // 2 bytes
        private short extHeaderFlags = 0; // 2 bytes
        //payload can be a frame in case of transcoded or a JSON in case of segmented
        private byte[] payload = null;
        private String stats = null;
        public VideoHeaderSizes headerSizes = null;
        public VideoHeaderLiveEvents headerLiveEvents = null, headerPlaybackEvents = null;
        /*
        public VideoHeaderNativeDataHeaders headerNativeDataHeaders;
        public VideoHeaderMotionEvents headerMotionEvents; //arcus only
        public VideoHeaderStreamInfo headerStreamInfo;
        public HeaderLocation headerLocation = null;
        */
        private byte[] unsupportedHeaders;
        
        public VideoCommand(String videoIdArg, byte[] bufHeader)
        {
            this(videoIdArg);
            if (bufHeader != null && bufHeader.length >= 36) {
                setTimeStamp(bufHeader, 16);
                frameCount = bytesToInt(bufHeader, 24);
                payloadSize = bytesToInt(bufHeader, 28);
                extHeaderSize = (short)(((bufHeader[33]&0xFF)<<8) | (bufHeader[32]&0xFF));
                extHeaderFlags = (short)(((bufHeader[35]&0xFF)<<8) | (bufHeader[34]&0xFF));
            }
        }
        
        public VideoCommand(String videoIdArg)
        {
            vId = videoIdArg;
            vIdBytes = UUIDtoByteArray(videoIdArg);
            extHeaderSize = 36;
        }
        
        public String getUUID()
        {
            return vId;
        }
        
        private void setTimeStamp(byte[] frame, int timeStampPos)
        {
            timeStamp |= frame[timeStampPos+7] & 0xFF;
            timeStamp <<= 8;
            timeStamp |= frame[timeStampPos+6] & 0xFF;
            timeStamp <<= 8;
            timeStamp |= frame[timeStampPos+5] & 0xFF;
            timeStamp <<= 8;
            timeStamp |= frame[timeStampPos+4] & 0xFF;
            timeStamp <<= 8;
            timeStamp |= frame[timeStampPos+3] & 0xFF;
            timeStamp <<= 8;
            timeStamp |= frame[timeStampPos+2] & 0xFF;
            timeStamp <<= 8;
            timeStamp |= frame[timeStampPos+1] & 0xFF;
            timeStamp <<= 8;
            timeStamp |= frame[timeStampPos] & 0xFF;
        }
        
        public void setTimestamp( long timestamp ){
            this.timeStamp = timestamp;
        }
        private int bytesToInt(byte[] buf, int start)
        {
            return ((buf[start+3]&0xFF)<<24) |((buf[start+2]&0xFF)<<16)
                |((buf[start+1]&0xFF)<<8)  | (buf[start]&0xFF);
        }
        
        private byte[] intToByteArray(int value) {
            return new byte[] {
                    (byte) value,
                    (byte)(value >>> 8),
                    (byte)(value >>> 16),
                    (byte)(value >>> 24)};
        }
        
        protected void setHeaderSizes(byte[] headerArg, boolean extendedResampling)
        {
            headerSizes = new VideoHeaderSizes(headerArg, extendedResampling);
        }
        
        protected void setHeaderSizes(int srcWidth, int srcHeight, int destWidth, int destHeight)
        {
            headerSizes = new VideoHeaderSizes(srcWidth, srcHeight, destWidth, destHeight);
        }
        
        protected void setHeaderLiveEvents(byte[] headerArg, int headerOffset)
        {
            headerLiveEvents = new VideoHeaderLiveEvents(headerArg, headerOffset);
        }
        
        protected void setHeaderLiveEvents(int curFlags, int chFlags)
        {
            headerLiveEvents = new VideoHeaderLiveEvents(curFlags, chFlags);
        }
        
        protected void setHeaderPlaybackEvents(byte[] headerArg, int headerOffset)
        {
            headerPlaybackEvents = new VideoHeaderLiveEvents(headerArg, headerOffset);
        }
/*
        protected void setNativeDataHeaders(byte[] headerArg, int headerOffset){
            headerNativeDataHeaders = new VideoHeaderNativeDataHeaders(headerArg, headerOffset);
        }

        protected void setMotionEventsHeader(byte[] headerArg, int headerOffset){
            headerMotionEvents = new VideoHeaderMotionEvents(headerArg, headerOffset);
        }

        protected void setHeaderStreamInfo(byte[] headerArg, int headerOffset)
        {
            headerStreamInfo = new VideoHeaderStreamInfo(headerArg, headerOffset);
        }
*/
        
        protected void setUsupportedHeaderBytes(byte[] unsupportedHeadersArg)
        {
            unsupportedHeaders = unsupportedHeadersArg;
        }

        public void setPayload(byte[] buffer)
        {
            // Just set our buffer to reference the incoming buffer
            // (assuming the incoming buffer will be unchanged!)
            payload = buffer;
            if (buffer != null) {
                payloadSize = buffer.length;
            } else {
                payloadSize = 0;
            }
        }
        
        public static byte[] UUIDtoByteArray(String uuidString)
        {
            if (uuidString == null)
                return null;
            
            UUID videoIdUUID = null;
            try {
                videoIdUUID = UUID.fromString(uuidString);
            } catch (Exception e) {
                return null;
            }
            
            long msb = videoIdUUID.getMostSignificantBits();
            long lsb = videoIdUUID.getLeastSignificantBits();
            
            byte[] uuidBytes = new byte[16];
            for (int i = 0; i < 8; i++)
                uuidBytes[i] = (byte) (msb >>> 8 * (7 - i));
            
            for (int i = 8; i < 16; i++)
                uuidBytes[i] = (byte) (lsb >>> 8 * (7 - i));
            
            return uuidBytes;
        }

        public byte[] getHeaders()
        {
            if (payload == null) {
                payload = new byte[payloadSize]; // creating empty frame
            }

            short totalHrdSize = 36;

            if( headerSizes != null ){
                totalHrdSize += 12*4;
                extHeaderFlags |= HEADER_SIZES;
            }

            extHeaderSize = totalHrdSize;

            byte[] raw = new byte[totalHrdSize + payloadSize];

            if (vIdBytes != null) {
                raw[0] = vIdBytes[3]; raw[1] = vIdBytes[2]; raw[2] = vIdBytes[1]; raw[3] = vIdBytes[0];
                raw[4] = vIdBytes[5]; raw[5] = vIdBytes[4];
                raw[6] = vIdBytes[7]; raw[7] = vIdBytes[6];
                System.arraycopy(vIdBytes, 					8, raw,  8, 8);
            }
            
            // TimeStamp , 16
            System.arraycopy(intToByteArray((int)timeStamp), 0, raw, 16, 4);
            System.arraycopy(intToByteArray((int)(timeStamp>>32)), 0, raw, 20, 4);

            // FrameCount, 24

            System.arraycopy(intToByteArray(payloadSize),	0, raw, 28, 4);
            raw[32] = (byte) extHeaderSize;
            raw[33] = (byte)(extHeaderSize >> 8);
            raw[34] = (byte) extHeaderFlags;
            raw[35] = (byte)(extHeaderFlags >> 8);

            int cnt = 36;

            if( (extHeaderFlags & HEADER_SIZES) != 0 ){
                byte[] hdrSize = headerSizes.toByteArray();
                System.arraycopy( hdrSize, 0, raw, cnt, hdrSize.length );
                cnt += hdrSize.length;
            }

            System.arraycopy(payload,				0, raw, cnt, payload.length);
            return raw;
        }
        
    };

    /** The main entry point. Given a jpeg file, generate a frame
      */
    public static void main(String[] args)
    {
        OutputStream outStream = null;

        if( args.length < 2  ){
            System.out.println("\r\nMakeHeader v0.1\r\n");
            System.out.println("Usage: MakeHeader <image.jpeg> VIDEO_UUID [timestamp]");
            System.out.println("Generates VIDEO_UUID.bin file containing frame with the provided image file");
            System.out.println("(If the timestamp parameter is present, sets the frame at this time.)\r\n\r\n");
            return;
        }

        try
        {
            VideoCommand frame = new VideoCommand( args[1] );
            frame.setHeaderSizes(800, 600, 800, 600 );

            Long timestamp = System.currentTimeMillis();
            if( args.length > 2 ){
                // We have a timestamp argument
                timestamp = Long.valueOf(args[2]);
            }
            frame.setTimestamp( timestamp );

            byte[] img = Files.readAllBytes( Paths.get(args[0]) );
            frame.setPayload( img );

            byte[] buf = frame.getHeaders();

            File file = new File( args[1] + ".bin");
            outStream = new FileOutputStream(file);
            outStream.write(buf, 0, buf.length );

            // Add the trailing /r/n/r/n 
            byte[] crlf = new byte[4];
            crlf[0] = 13;
            crlf[1] = 10;
            crlf[2] = 13;
            crlf[3] = 10;

            outStream.write( crlf, 0, 4 );
            outStream.close();

            System.out.println("Frame created.");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}