package com.ctrip.xpipe.redis.protocal.protocal;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.ctrip.xpipe.api.codec.Codec;
import com.ctrip.xpipe.redis.protocal.RedisClientProtocol;
import com.ctrip.xpipe.utils.StringUtil;

import io.netty.buffer.ByteBuf;


/**
 * @author wenchao.meng
 *
 * 2016年3月24日 下午6:29:33
 */
public abstract class AbstractRedisClientProtocol<T> extends AbstractRedisProtocol implements RedisClientProtocol<T>{
	
	protected final T payload;
	
	protected final boolean logRead;

	protected final boolean logWrite;

	private ByteArrayOutputStream baous = new ByteArrayOutputStream();
	private CRLF_STATE 			  crlfState = CRLF_STATE.CONTENT;
	

	public AbstractRedisClientProtocol() {
		this(null, true, true);
	}
	
	public AbstractRedisClientProtocol(T payload, boolean logRead, boolean logWrite) {
		this.payload = payload;
		this.logRead = logRead;
		this.logWrite = logWrite;
	}
	

	
	@Override
	public byte[] format(){
		
		byte [] toWrite = getWriteBytes();
		
		if(logWrite && logger.isDebugEnabled()){
			
			logger.info("[getWriteBytes]" + getPayloadAsString());
		}
		return toWrite;
	}
	
	protected String getPayloadAsString() {
		
		String payloadString = payload.toString();
		if(payload instanceof String[]){
			payloadString = StringUtil.join(" ", (String[])payload); 
		}
		return  payloadString;
	}

	protected abstract byte[] getWriteBytes();

	/**
	 * @param byteBuf
	 * @return 结束则返回对应byte[], 否则返回null
	 * @throws IOException 
	 */
	protected byte[] readTilCRLF(ByteBuf byteBuf){
		
		int readable = byteBuf.readableBytes();
		for(int i=0; i < readable ;i++){
			
			byte data = byteBuf.readByte();
			baous.write(data);
			switch(data){
				case '\r':
					crlfState = CRLF_STATE.CR;
					break;
				case '\n':
					if(crlfState == CRLF_STATE.CR){
						crlfState = CRLF_STATE.CRLF;
						break;
					}
				default:
					crlfState = CRLF_STATE.CONTENT;
					break;
			}
			
			if(crlfState == CRLF_STATE.CRLF){
				return baous.toByteArray();
			}
		}
		
		return null;
	}

	
	protected  String readTilCRLFAsString(ByteBuf byteBuf, Charset charset){
		
		byte []bytes = readTilCRLF(byteBuf);
		if(bytes == null){
			return null;
		}
		String ret = new String(bytes, charset);
		if(logger.isDebugEnabled() && logRead){
			logger.info("[readTilCRLFAsString]" + ret.trim());
		}
		return ret;
		
	}

	protected  String readTilCRLFAsString(ByteBuf byteBuf){

		return readTilCRLFAsString(byteBuf, Codec.defaultCharset);
	}

	protected byte[] getRequestBytes(Byte sign, Integer integer) {
		
		StringBuilder sb = new StringBuilder();
		sb.append((char)sign.byteValue());
		sb.append(integer);
		sb.append("\r\n");
		return sb.toString().getBytes();
	}

	protected byte[] getRequestBytes(Byte sign, String ... commands) {
		return getRequestBytes(Codec.defaultCharset, sign, commands);
	}

	protected byte[] getRequestBytes(Charset charset, Byte sign, String ... commands) {
		
		StringBuilder sb = new StringBuilder();
		if(sign != null){
			sb.append((char)sign.byteValue());
		}
		sb.append(StringUtil.join(" ",commands));
		sb.append("\r\n");
		return sb.toString().getBytes(charset);
	}


	protected byte[] getRequestBytes(String ... commands) {
		return getRequestBytes(Codec.defaultCharset, commands);
	}

	protected byte[] getRequestBytes(Charset charset, String ... commands) {
		return getRequestBytes(charset, null, commands);
	}

	
	@Override
	public T getPayload() {
		return payload;
	}



	public enum CRLF_STATE{
		CR,
		CRLF,
		CONTENT
	}
}
