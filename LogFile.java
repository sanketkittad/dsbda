package PS1Package;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class LogFile {

	public static void main(String [] args)throws Exception{
		Configuration c=new Configuration();
		String[] filename=new GenericOptionsParser(c,args).getRemainingArgs();
		Path input=new Path(filename[0]);
		Path output=new Path(filename[1]);
		Job j=new Job(c,"logfile");
		j.setJarByClass(LogFile.class);
		j.setMapperClass(MapperLogFile.class);
		j.setReducerClass(ReducerLogFile.class);
		j.setOutputKeyClass(Text.class);
		j.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(j, input);
		FileOutputFormat.setOutputPath(j,output);
		System.exit(j.waitForCompletion(true)?0:1);
	}
	
	public static class MapperLogFile extends Mapper<LongWritable,Text,Text,IntWritable>{
		public void map(LongWritable key,Text value,Context con) throws IOException,InterruptedException{
			String lineText=value.toString();
			String[] lines=lineText.split("\n");
			
			for(String currLine: lines){
				String[] splitLine=currLine.split(",");
				String ipadress=splitLine[1];
				String intimeString=splitLine[5],outtimeString=splitLine[7];
				String intime=intimeString.split(" ")[1],outtime=outtimeString.split(" ")[1];
				SimpleDateFormat format=new SimpleDateFormat("HH:mm:ss");
				Date indate = null;
				try {
					indate = format.parse(intime);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Date outdate = null;
				try {
					outdate = format.parse(outtime);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				long difference=outdate.getTime()-indate.getTime();
				int minutes= (int) ((difference / (60 * 1000)) % 60);
				Text outputKey=new Text(ipadress.toUpperCase().trim());
				IntWritable outvalue=new IntWritable(minutes);
				con.write(outputKey, outvalue);
			}
			
		}
	}
		public static class ReducerLogFile extends Reducer<Text,IntWritable,Text,IntWritable>{
			int max_value=0;
			HashMap<String,Integer> timeValues=new HashMap<>();
			public void reduce(Text ip,Iterable<IntWritable> times,Context con){
				int totalTime=0;
				for(IntWritable num: times){
					totalTime+=num.get();
				}
				if(totalTime>max_value){
					max_value=totalTime;
				}
				String ipadress=ip.toString();
				timeValues.put(ip.toString(),(totalTime));
			}
			@Override
			protected void cleanup(Context con) throws InterruptedException, IOException{
				for(String ip: timeValues.keySet()){
					if(timeValues.get(ip)==max_value){
						Text iptext=new Text(ip);
						IntWritable count=new IntWritable(timeValues.get(ip));
						con.write(iptext, count);
					}
				}
			}
			
		}
	}

