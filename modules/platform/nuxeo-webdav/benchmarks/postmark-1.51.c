/*
Written by Jeffrey Katcher under contract to Network Appliance.
Copyright (C) 1997-2001
Network Appliance, Inc.

This code has been successfully compiled and run by Network
Appliance on various platforms, including Solaris 2 on an Ultra-170,
and Windows NT on a Compaq ProLiant. However, this PostMark source
code is distributed under the Artistic License appended to the end
of this file. As such, no support is provided. However, please report
any errors to the author, Jeffrey Katcher <katcher@netapp.com>, or to
Andy Watson <watson@netapp.com>.

Versions:
1.00 - Original release - 8/17/97

1.01 - Fixed endless loop on EOF,
       Divide by zero when file_size_high=file_size_low - 10/29/97
       (Thanks to Chuck Murnane)

1.1 - Added new commands to distribute work across multiple directories
      and/or file systems and multiple work subdirectories.

      Changed set location command (+,-) to allow file systems & weights
      Added set subdirectories command and code to distribute work across
         multiple subdirectories
      Added file redirect to show and run commands
      Improved help system - 4/8/98

1.11 - Fixed unfortunate problem where read_file opens in append mode thus
       avoiding actual reads.  (Thanks to Kent Peacock)

1.12 - Changed bytes read and written to float.  Hopefully this will avoid
       overflow when very large file sizes are used.

1.13 - Added terse report option allowing results to be easily included in
       other things.  (Thanks to Walter Wong)
       Also tweaked help code to allow partial matches

1.14 - Automatically stop run if work files are depleted

1.5 - Many people (most recently Michael Flaster) have emphasized that the
      pseudo-random number generator was more pseudo than random.  After
      a review of the literature and extensive benchmarking, I've replaced
      the previous PRNG with the Mersenne Twister.  While an excellent PRNG,
      it retains much of the performance of the previous implementation.
      URL: http://www.math.keio.ac.jp/~matumoto/emt.html
      Also changed MB definition to 1024KB, tweaked show command

1.51 - Added command to load config file from CLI
*/

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <fcntl.h>

#define PM_VERSION "v1.51 : 8/14/01"

#ifdef _WIN32
#include <io.h>
#include <direct.h>

#define GETWD(x) getcwd(x,MAX_LINE)
#define MKDIR(x) mkdir(x)
#define SEPARATOR "\\"
#else
extern char *getwd();

#define GETWD(x) getwd(x)
#define MKDIR(x) mkdir(x,0700)
#define SEPARATOR "/"
#endif

#define MAX_LINE 255
#define MAX_FILENAME 80

#define KILOBYTE 1024
#define MEGABYTE (KILOBYTE*KILOBYTE)

#define PROMPT "pm>"

typedef struct { /* ADT for table of CLI commands */
   char *name;    /* name of command */
   int (*func)(); /* pointer to callback function */
   char *help;    /* descriptive help string */
} cmd;

extern int cli_set_size();
extern int cli_set_number();
extern int cli_set_seed();
extern int cli_set_transactions();
extern int cli_set_location();
extern int cli_set_subdirs();
extern int cli_set_read();
extern int cli_set_write();
extern int cli_set_buffering();
extern int cli_set_bias_read();
extern int cli_set_bias_create();
extern int cli_set_report();

extern int cli_run();
extern int cli_load();
extern int cli_show();
extern int cli_help();
extern int cli_quit();

cmd command_list[]={ /* table of CLI commands */
   {"set size",cli_set_size,"Sets low and high bounds of files"},
   {"set number",cli_set_number,"Sets number of simultaneous files"},
   {"set seed",cli_set_seed,"Sets seed for random number generator"},
   {"set transactions",cli_set_transactions,"Sets number of transactions"},
   {"set location",cli_set_location,"Sets location of working files"},
   {"set subdirectories",cli_set_subdirs,"Sets number of subdirectories"},
   {"set read",cli_set_read,"Sets read block size"},
   {"set write",cli_set_write,"Sets write block size"},
   {"set buffering",cli_set_buffering,"Sets usage of buffered I/O"},
   {"set bias read",cli_set_bias_read,
      "Sets the chance of choosing read over append"},
   {"set bias create",cli_set_bias_create,
      "Sets the chance of choosing create over delete"},
   {"set report",cli_set_report,"Choose verbose or terse report format"},
   {"run",cli_run,"Runs one iteration of benchmark"},
   {"load",cli_load,"Read configuration file"},
   {"show",cli_show,"Displays current configuration"},
   {"help",cli_help,"Prints out available commands"},
   {"quit",cli_quit,"Exit program"},
   NULL
};

extern void verbose_report();
extern void terse_report();
void (*reports[])()={verbose_report,terse_report};

/* Counters */
int files_created;  /* number of files created */
int files_deleted;  /* number of files deleted */
int files_read;     /* number of files read */
int files_appended; /* number of files appended */
float bytes_written; /* number of bytes written to files */
float bytes_read;    /* number of bytes read from files */

/* Configurable Parameters */
int file_size_low=500;
int file_size_high=10000;       /* file size: fixed or random within range */
int simultaneous=500;           /* simultaneous files */
int seed=42;                    /* random number generator seed */
int transactions=500;           /* number of transactions */
int subdirectories=0;		/* Number of subdirectories */
int read_block_size=512;        /* I/O block sizes */
int write_block_size=512;
int bias_read=5;                /* chance of picking read over append */
int bias_create=5;              /* chance of picking create over delete */
int buffered_io=1;              /* use C library buffered I/O */
int report=0;                   /* 0=verbose, 1=terse report format */

/* Working Storage */
char *file_source; /* pointer to buffer of random text */

typedef struct {
   char name[MAX_FILENAME+1]; /* name of individual file */
   int size;                  /* current size of file, 0 = unused file slot */
} file_entry;

file_entry *file_table; /* table of files in use */
int file_allocated;     /* pointer to last allocated slot in file_table */

typedef struct file_system_struct {
   file_entry system;
   struct file_system_struct *next,*prev;
} file_system;

file_system *file_systems; /* table of file systems/directories to use */
int file_system_weight;    /* sum of weights for all file systems */
int file_system_count;     /* number of configured file systems */
char **location_index;     /* weighted index of file systems */

char *read_buffer; /* temporary space for reading file data into */

#define RND(x) ((x>0)?(genrand() % (x)):0)
extern unsigned long genrand();
extern void sgenrand();

/* converts integer values to byte/kilobyte/megabyte strings */
char *scale(i)
int i;
{
   static char buffer[MAX_LINE]; /* storage for current conversion */

   if (i/MEGABYTE)
      sprintf(buffer,"%.2f megabytes",(float)i/MEGABYTE);
   else
      if (i/KILOBYTE)
         sprintf(buffer,"%.2f kilobytes",(float)i/KILOBYTE);
      else
         sprintf(buffer,"%d bytes",i);

   return(buffer);
}

/* converts float values to byte/kilobyte/megabyte strings */
char *scalef(i)
float i;
{
   static char buffer[MAX_LINE]; /* storage for current conversion */

   if (i/(float)MEGABYTE>1)
      sprintf(buffer,"%.2f megabytes",i/(float)MEGABYTE);
   else
      if (i/(float)KILOBYTE)
         sprintf(buffer,"%.2f kilobytes",i/(float)KILOBYTE);
      else
         sprintf(buffer,"%f bytes",i);

   return(buffer);
}

/* UI callback for 'set size' command - sets range of file sizes */
int cli_set_size(param)
char *param; /* remainder of command line */
{
   char *token;
   int size;

   if (param && (size=atoi(param))>0)
      {
      file_size_low=size;
      if ((token=strchr(param,' ')) && (size=atoi(token))>0 &&
         size>=file_size_low)
         file_size_high=size;
      else
         file_size_high=file_size_low;
      }
   else
      fprintf(stderr,"Error: no file size low or high bounds specified\n");

   return(1);
}

int cli_generic_int(var,param,error)
int *var; /* pointer to variable to set */
char *param; /* remainder of command line */
char *error; /* error message */
{
   int value;

   if (param && (value=atoi(param))>0)
      *var=value;
   else
      fprintf(stderr,"Error: %s\n",error);

   return(1);
}

/* UI callback for 'set number' command - sets number of files to create */
int cli_set_number(param)
char *param; /* remainder of command line */
{
   return(cli_generic_int(&simultaneous,param,"invalid number of files"));
}

/* UI callback for 'set seed' command - initial value for random number gen */
int cli_set_seed(param)
char *param; /* remainder of command line */
{
   return(cli_generic_int(&seed,param,"invalid seed for random numbers"));
}

/* UI callback for 'set transactions' - configure number of transactions */
int cli_set_transactions(param)
char *param; /* remainder of command line */
{
   return(cli_generic_int(&transactions,param,"no transactions specified"));
}

int parse_weight(params)
char *params;
{
   int weight=1;
   char *split;

   if (split=strrchr(params,' '))
      {
      *split='\0';
      if ((weight=atoi(split+1))<=0)
         {
         fprintf(stderr,"Error: ignoring invalid weight '%s'\n",split+1);
         weight=1;
         }
      }

   return(weight);
}

void add_location(params,weight)
char *params;
int weight;
{
   file_system *new_file_system;

   if (new_file_system=(file_system *)calloc(1,sizeof(file_system)))
      {
      strcpy(new_file_system->system.name,params);
      new_file_system->system.size=weight;

      if (file_systems)
         {
         new_file_system->prev=file_systems->prev;
         file_systems->prev->next=new_file_system;
         file_systems->prev=new_file_system;
         }
      else
         {
         new_file_system->prev=new_file_system;
         file_systems=new_file_system;
         }

      file_system_weight+=weight;
      file_system_count++;
      }
}

void delete_location(loc_name)
char *loc_name;
{
   file_system *traverse;

   for (traverse=file_systems; traverse; traverse=traverse->next)
      if (!strcmp(traverse->system.name,loc_name))
         {
         file_system_weight-=traverse->system.size;
         file_system_count--;

         if (file_systems->prev==file_systems)
            {
            free(file_systems);
            file_systems=NULL;
            }
         else
            {
            if (file_systems->prev==traverse)
               file_systems->prev=traverse->prev;

            if (traverse==file_systems)
               file_systems=file_systems->next;
            else
               traverse->prev->next=traverse->next;

            if (traverse->next)
               traverse->next->prev=traverse->prev;

            free(traverse);
            }

         break;
         }

   if (!traverse)
      fprintf(stderr,"Error: cannot find location '%s'\n",loc_name);
}

void delete_locations()
{
   file_system *next;

   while (file_systems)
      {
      next=file_systems->next;
      free(file_systems);
      file_systems=next;
      }

   file_system_weight=0;
   file_system_count=0;
}

/* UI callback for 'set location' - configure current working directory */
int cli_set_location(param)
char *param; /* remainder of command line */
{
   if (param)
      {
      switch (*param)
         {
         case '+': /* add location to list */
            add_location(param+1,parse_weight(param+1));
            break;

         case '-': /* remove location from list */
            delete_location(param+1);
            break;

         default:
            delete_locations();
            add_location(param,parse_weight(param));
         }
      }
   else
      fprintf(stderr,"Error: no directory name specified\n");

   return(1);
}

/* UI callback for 'set subdirectories' - configure number of subdirectories */
int cli_set_subdirs(param)
char *param; /* remainder of command line */
{
   return(cli_generic_int(&subdirectories,param,
      "invalid number of subdirectories"));
}

/* UI callback for 'set read' - configure read block size (integer) */
int cli_set_read(param)
char *param; /* remainder of command line */
{
   return(cli_generic_int(&read_block_size,param,"invalid read block size"));
}

/* UI callback for 'set write' - configure write block size (integer) */
int cli_set_write(param)
char *param; /* remainder of command line */
{
   return(cli_generic_int(&write_block_size,param,"invalid write block size"));
}

/* UI callback for 'set buffering' - sets buffering mode on or off
   - true = buffered I/O (default), false = raw I/O */
int cli_set_buffering(param)
char *param; /* remainder of command line */
{
   if (param && (!strcmp(param,"true") || !strcmp(param,"false")))
      buffered_io=(!strcmp(param,"true"))?1:0;
   else
      fprintf(stderr,"Error: no buffering mode (true/false) specified\n");

   return(1);
}

/* UI callback for 'set bias read' - sets probability of read vs. append */
int cli_set_bias_read(param)
char *param; /* remainder of command line */
{
   int value;

   if (param && (value=atoi(param))>=-1 && value<=10)
      bias_read=value;
   else
      fprintf(stderr,
        "Error: no bias specified (0-10 for greater chance,-1 to disable)\n");

   return(1);
}

/* UI callback for 'set bias create' - sets probability of create vs. delete */
int cli_set_bias_create(param)
char *param; /* remainder of command line */
{
   int value;

   if (param && (value=atoi(param))>=-1 && value<=10)
      bias_create=value;
   else
      fprintf(stderr,
         "Error: no bias specified (0-10 for greater chance,-1 to disable)\n");

   return(1);
}

/* UI callback for 'set report' - chooses verbose or terse report formats */
int cli_set_report(param)
char *param; /* remainder of command line */
{
   int match=0;

   if (param)
      {
      if (!strcmp(param,"verbose"))
         report=0;
      else
         if (!strcmp(param,"terse"))
            report=1;
         else
            match=-1;
      }

   if (!param || match==-1)
      fprintf(stderr,"Error: either 'verbose' or 'terse' required\n");

   return(1);
}

/* populate file source buffer with 'size' bytes of readable randomness */
char *initialize_file_source(size)
int size; /* number of bytes of junk to create */
{
   char *new_source;
   int i;

   if ((new_source=(char *)malloc(size))==NULL) /* allocate buffer */
      fprintf(stderr,"Error: failed to allocate source file of size %d\n",size);
   else
      for (i=0; i<size; i++) /* file buffer with junk */
         new_source[i]=32+RND(95);

   return(new_source);
}

/* returns differences in times -
   1 second is the minimum to avoid divide by zero errors */
time_t diff_time(t1,t0)
time_t t1;
time_t t0;
{
   return((t1-=t0)?t1:1);
}

/* prints out results from running transactions */
void verbose_report(fp,end_time,start_time,t_end_time,t_start_time,deleted)
FILE *fp;
time_t end_time,start_time,t_end_time,t_start_time; /* timers from run */
int deleted; /* files deleted back-to-back */
{
   time_t elapsed,t_elapsed;
   int interval;

   elapsed=diff_time(end_time,start_time);
   t_elapsed=diff_time(t_end_time,t_start_time);

   fprintf(fp,"Time:\n");
   fprintf(fp,"\t%d seconds total\n",elapsed);
   fprintf(fp,"\t%d seconds of transactions (%d per second)\n",t_elapsed,
      transactions/t_elapsed);

   fprintf(fp,"\nFiles:\n");
   fprintf(fp,"\t%d created (%d per second)\n",files_created,
      files_created/elapsed);

   interval=diff_time(t_start_time,start_time);
   fprintf(fp,"\t\tCreation alone: %d files (%d per second)\n",simultaneous,
      simultaneous/interval);
   fprintf(fp,"\t\tMixed with transactions: %d files (%d per second)\n",
      files_created-simultaneous,(files_created-simultaneous)/t_elapsed);
   fprintf(fp,"\t%d read (%d per second)\n",files_read,files_read/t_elapsed);
   fprintf(fp,"\t%d appended (%d per second)\n",files_appended,
      files_appended/t_elapsed);
   fprintf(fp,"\t%d deleted (%d per second)\n",files_created,
      files_created/elapsed);

   interval=diff_time(end_time,t_end_time);
   fprintf(fp,"\t\tDeletion alone: %d files (%d per second)\n",deleted,
      deleted/interval);
   fprintf(fp,"\t\tMixed with transactions: %d files (%d per second)\n",
      files_deleted-deleted,(files_deleted-deleted)/t_elapsed);

   fprintf(fp,"\nData:\n");
   fprintf(fp,"\t%s read ",scalef(bytes_read));
   fprintf(fp,"(%s per second)\n",scalef(bytes_read/(float)elapsed));
   fprintf(fp,"\t%s written ",scalef(bytes_written));
   fprintf(fp,"(%s per second)\n",scalef(bytes_written/(float)elapsed));
}

void terse_report(fp,end_time,start_time,t_end_time,t_start_time,deleted)
FILE *fp;
time_t end_time,start_time,t_end_time,t_start_time; /* timers from run */
int deleted; /* files deleted back-to-back */
{
   time_t elapsed,t_elapsed;
   int interval;

   elapsed=diff_time(end_time,start_time);
   t_elapsed=diff_time(t_end_time,t_start_time);
   interval=diff_time(t_start_time,start_time);

   fprintf(fp,"%d %d %.2f ", elapsed, t_elapsed,
      (float)transactions/t_elapsed);
   fprintf(fp, "%.2f %.2f %.2f ", (float)files_created/elapsed,
      (float)simultaneous/interval,
      (float)(files_created-simultaneous)/t_elapsed);
   fprintf(fp, "%.2f %.2f ", (float)files_read/t_elapsed,
      (float)files_appended/t_elapsed);
   fprintf(fp, "%.2f %.2f %.2f ", (float)files_created/elapsed,
      (float)deleted/interval,
      (float)(files_deleted-deleted)/t_elapsed);
   fprintf(fp, "%.2f %.2f\n", (float)bytes_read/elapsed,
      (float)bytes_written/elapsed);
}

/* returns file_table entry of unallocated file
   - if not at end of table, then return next entry
   - else search table for gaps */
int find_free_file()
{
   int i;

   if (file_allocated<simultaneous<<1 && file_table[file_allocated].size==0)
      return(file_allocated++);
   else /* search entire table for holes */
      for (i=0; i<simultaneous<<1; i++)
         if (file_table[i].size==0)
            {
            file_allocated=i;
            return(file_allocated++);
            }

   return(-1); /* return -1 only if no free files found */
}

/* write 'size' bytes to file 'fd' using unbuffered I/O and close file */
void write_blocks(fd,size)
int fd;
int size;   /* bytes to write to file */
{
   int offset=0; /* offset into file */
   int i;

   /* write even blocks */
   for (i=size; i>=write_block_size;
      i-=write_block_size,offset+=write_block_size)
      write(fd,file_source+offset,write_block_size);

   write(fd,file_source+offset,i); /* write remainder */

   bytes_written+=size; /* update counter */

   close(fd);
}

/* write 'size' bytes to file 'fp' using buffered I/O and close file */
void fwrite_blocks(fp,size)
FILE *fp;
int size;   /* bytes to write to file */
{
   int offset=0; /* offset into file */
   int i;

   /* write even blocks */
   for (i=size; i>=write_block_size;
      i-=write_block_size,offset+=write_block_size)
      fwrite(file_source+offset,write_block_size,1,fp);

   fwrite(file_source+offset,i,1,fp); /* write remainder */

   bytes_written+=size; /* update counter */

   fclose(fp);
}

void create_file_name(dest)
char *dest;
{
   char conversion[MAX_LINE+1];

   *dest='\0';
   if (file_system_count)
      {
      strcat(dest,
         location_index[(file_system_count==1)?0:RND(file_system_weight)]);
      strcat(dest,SEPARATOR);
      }

   if (subdirectories>1)
      {
      sprintf(conversion,"s%d%s",RND(subdirectories),SEPARATOR);
      strcat(dest,conversion);
      }

   sprintf(conversion,"%d",++files_created);
   strcat(dest,conversion);
}

/* creates new file of specified length and fills it with data */
void create_file(buffered)
int buffered; /* 1=buffered I/O (default), 0=unbuffered I/O */
{
   FILE *fp=NULL;
   int fd=-1;
   int free_file; /* file_table slot for new file */

   if ((free_file=find_free_file())!=-1) /* if file space is available */
      { /* decide on name and initial length */
      create_file_name(file_table[free_file].name);

      file_table[free_file].size=
         file_size_low+RND(file_size_high-file_size_low);

      if (buffered)
         fp=fopen(file_table[free_file].name,"w");
      else
         fd=open(file_table[free_file].name,O_RDWR|O_CREAT,0644);

      if (fp || fd!=-1)
         {
         if (buffered)
            fwrite_blocks(fp,file_table[free_file].size);
         else
            write_blocks(fd,file_table[free_file].size);
         }
      else
         fprintf(stderr,"Error: cannot open '%s' for writing\n",
            file_table[free_file].name);
      }
}

/* deletes specified file from disk and file_table */
void delete_file(number)
int number;
{
   if (file_table[number].size)
      {
      if (remove(file_table[number].name))
         fprintf(stderr,"Error: Cannot delete '%s'\n",file_table[number].name);
      else
         { /* reset entry in file_table and update counter */
         file_table[number].size=0;
         files_deleted++;
         }
      }
}

/* reads entire specified file into temporary buffer */
void read_file(number,buffered)
int number;   /* number of file to read (from file_table) */
int buffered; /* 1=buffered I/O (default), 0=unbuffered I/O */
{
   FILE *fp=NULL;
   int fd=-1;
   int i;

   if (buffered)
      fp=fopen(file_table[number].name,"r");
   else
      fd=open(file_table[number].name,O_RDONLY,0644);

   if (fp || fd!=-1)
      { /* read as many blocks as possible then read the remainder */
      if (buffered)
         {
         for (i=file_table[number].size; i>=read_block_size; i-=read_block_size)
            fread(read_buffer,read_block_size,1,fp);

         fread(read_buffer,i,1,fp);

         fclose(fp);
         }
      else
         {
         for (i=file_table[number].size; i>=read_block_size; i-=read_block_size)
            read(fd,read_buffer,read_block_size);

         read(fd,read_buffer,i);

         close(fd);
         }

      /* increment counters to record transaction */
      bytes_read+=file_table[number].size;
      files_read++;
      }
   else
      fprintf(stderr,"Error: cannot open '%s' for reading\n",
         file_table[number].name);
}

/* appends random data to a chosen file up to the maximum configured length */
void append_file(number,buffered)
int number;   /* number of file (from file_table) to append date to */
int buffered; /* 1=buffered I/O (default), 0=unbuffered I/O */
{
   FILE *fp=NULL;
   int fd=-1;
   int block; /* size of data to append */

   if (file_table[number].size<file_size_high)
      {
      if (buffered)
         fp=fopen(file_table[number].name,"a");
      else
         fd=open(file_table[number].name,O_RDWR|O_APPEND,0644);

      if ((fp || fd!=-1) && file_table[number].size<file_size_high)
         {
         block=RND(file_size_high-file_table[number].size)+1;

         if (buffered)
            fwrite_blocks(fp,block);
         else
            write_blocks(fd,block);

         file_table[number].size+=block;
         files_appended++;
         }
      else
         fprintf(stderr,"Error: cannot open '%s' for append\n",
            file_table[number].name);
      }
}

/* finds and returns the offset of a file that is in use from the file_table */
int find_used_file() /* only called after files are created */
{
   int used_file;

   while (file_table[used_file=RND(simultaneous<<1)].size==0)
      ;

   return(used_file);
}

/* reset global counters - done before each test run */
void reset_counters()
{
   files_created=0;
   files_deleted=0;
   files_read=0;
   files_appended=0;
   bytes_written=0;
   bytes_read=0;
}

/* perform the configured number of file transactions
   - a transaction consisted of either a read or append and either a
     create or delete all chosen at random */
int run_transactions(buffered)
int buffered; /* 1=buffered I/O (default), 0=unbuffered I/O */
{
   int percent; /* one tenth of the specified transactions */
   int i;

   percent=transactions/10;
   for (i=0; i<transactions; i++)
      {
      if (files_created==files_deleted)
         {
         printf("out of files!\n");
         printf("For this workload, either increase the number of files or\n");
         printf("decrease the number of transactions.\n");
         break;
         }

      if (bias_read!=-1) /* if read/append not locked out... */
         {
         if (RND(10)<bias_read) /* read file */
            read_file(find_used_file(),buffered);
         else /* append file */
            append_file(find_used_file(),buffered);
         }

      if (bias_create!=-1) /* if create/delete not locked out... */
         {
         if (RND(10)<bias_create) /* create file */
            create_file(buffered);
         else /* delete file */
            delete_file(find_used_file());
         }

      if ((i % percent)==0) /* if another tenth of the work is done...*/
         {
         putchar('.'); /* print progress indicator */
         fflush(stdout);
         }
      }

   return(transactions-i);
}

char **build_location_index(list,weight)
file_system *list;
int weight;
{
   char **index;
   int count;
   int i=0;

   if ((index=(char **)calloc(1,weight*sizeof(char *)))==NULL)
      fprintf(stderr,"Error: cannot build weighted index of locations\n");
   else
      for (; list; list=list->next)
         for (count=0; count<list->system.size; count++)
            index[i++]=list->system.name;

   return(index);
}

void create_subdirectories(dir_list,base_dir,subdirs)
file_system *dir_list;
char *base_dir;
int subdirs;
{
   char dir_name[MAX_LINE+1]; /* buffer holding subdirectory names */
   char save_dir[MAX_LINE+1];
   int i;

   if (dir_list)
      {
      for (; dir_list; dir_list=dir_list->next)
         create_subdirectories(NULL,dir_list->system.name,subdirs);
      }
   else
      {
      if (base_dir)
         sprintf(save_dir,"%s%s",base_dir,SEPARATOR);
      else
         *save_dir='\0';

      for (i=0; i<subdirs; i++)
         {
         sprintf(dir_name,"%ss%d",save_dir,i);
         MKDIR(dir_name);
         }
      }
}

void delete_subdirectories(dir_list,base_dir,subdirs)
file_system *dir_list;
char *base_dir;
int subdirs;
{
   char dir_name[MAX_LINE+1]; /* buffer holding subdirectory names */
   char save_dir[MAX_LINE+1];
   int i;

   if (dir_list)
      {
      for (; dir_list; dir_list=dir_list->next)
         delete_subdirectories(NULL,dir_list->system.name,subdirs);
      }
   else
      {
      if (base_dir)
         sprintf(save_dir,"%s%s",base_dir,SEPARATOR);
      else
         *save_dir='\0';

      for (i=0; i<subdirs; i++)
         {
         sprintf(dir_name,"%ss%d",save_dir,i);
         rmdir(dir_name);
         }
      }
}

/* CLI callback for 'run' - benchmark execution loop */
int cli_run(param) /* none */
char *param; /* unused */
{
   time_t start_time,t_start_time,t_end_time,end_time; /* elapsed timers */
   int delete_base; /* snapshot of deleted files counter */
   FILE *fp=NULL; /* file descriptor for directing output */
   int incomplete;
   int i; /* generic iterator */

   reset_counters(); /* reset counters before each run */

   sgenrand(seed); /* initialize random number generator */

   /* allocate file space and fill with junk */
   file_source=initialize_file_source(file_size_high<<1);

   /* allocate read buffer */
   read_buffer=(char *)malloc(read_block_size);

   /* allocate table of files at 2 x simultaneous files */
   file_allocated=0;
   if ((file_table=(file_entry *)calloc(simultaneous<<1,sizeof(file_entry)))==
      NULL)
      fprintf(stderr,"Error: Failed to allocate table for %d files\n",
         simultaneous<<1);

   if (file_system_count>0)
      location_index=build_location_index(file_systems,file_system_weight);

   /* create subdirectories if necessary */
   if (subdirectories>1)
      {
      printf("Creating subdirectories...");
      fflush(stdout);
      create_subdirectories(file_systems,NULL,subdirectories);
      printf("Done\n");
      }

   time(&start_time); /* store start time */

   /* create files in specified directory until simultaneous number */
   printf("Creating files...");
   fflush(stdout);
   for (i=0; i<simultaneous; i++)
      create_file(buffered_io);
   printf("Done\n");

   printf("Performing transactions");
   fflush(stdout);
   time(&t_start_time);
   incomplete=run_transactions(buffered_io);
   time(&t_end_time);
   if (!incomplete)
      printf("Done\n");

   /* delete remaining files */
   printf("Deleting files...");
   fflush(stdout);
   delete_base=files_deleted;
   for (i=0; i<simultaneous<<1; i++)
      delete_file(i);
   printf("Done\n");

   /* print end time and difference, transaction numbers */
   time(&end_time);

   /* delete previously created subdirectories */
   if (subdirectories>1)
      {
      printf("Deleting subdirectories...");
      fflush(stdout);
      delete_subdirectories(file_systems,NULL,subdirectories);
      printf("Done\n");
      }

   if (location_index)
      {
      free(location_index);
      location_index=NULL;
      }

   if (param)
      if ((fp=fopen(param,"a"))==NULL)
         fprintf(stderr,"Error: Cannot direct output to file '%s'\n",param);

   if (!fp)
      fp=stdout;

   if (!incomplete)
      reports[report](fp,end_time,start_time,t_end_time,t_start_time,
         files_deleted-delete_base);

   if (param && fp!=stdout)
      fclose(fp);

   /* free resources allocated for this run */
   free(file_table);
   free(read_buffer);
   free(file_source);

   return(1); /* return 1 unless exit requested, then return 0 */
}

/* CLI callback for 'load' - read configuration file */
int cli_load(param)
char *param;
{
   char buffer[MAX_LINE+1]; /* storage for input command line */

   if (param)
      read_config_file(param,buffer,0);
   else
      fprintf(stderr,"Error: no configuration file specified\n");

   return(1); /* return 1 unless exit requested, then return 0 */
}

/* CLI callback for 'show' - print values of configuration variables */
int cli_show(param)
char *param; /* optional: name of output file */
{
   char current_dir[MAX_LINE+1]; /* buffer containing working directory */
   file_system *traverse;
   FILE *fp=NULL; /* file descriptor for directing output */

   if (param)
      if ((fp=fopen(param,"a"))==NULL)
         fprintf(stderr,"Error: Cannot direct output to file '%s'\n",param);

   if (!fp)
      fp=stdout;

   fprintf(fp,"Current configuration is:\n");
   fprintf(fp,"The base number of files is %d\n",simultaneous);
   fprintf(fp,"Transactions: %d\n",transactions);

   if (file_size_low!=file_size_high)
      {
      fprintf(fp,"Files range between %s ",scale(file_size_low));
      fprintf(fp,"and %s in size\n",scale(file_size_high));
      }
   else
      fprintf(fp,"Files are %s in size\n",scale(file_size_low));

   fprintf(fp,"Working director%s: %s\n",(file_system_count>1)?"ies":"y",
      (file_system_count==0)?GETWD(current_dir):"");

   for (traverse=file_systems; traverse; traverse=traverse->next)
      printf("\t%s (weight=%d)\n",traverse->system.name,traverse->system.size);

   if (subdirectories>0)
      fprintf(fp,"%d subdirector%s will be used\n",subdirectories,
         (subdirectories==1)?"y":"ies");

   fprintf(fp,"Block sizes are: read=%s, ",scale(read_block_size));
   fprintf(fp,"write=%s\n",scale(write_block_size));
   fprintf(fp,"Biases are: read/append=%d, create/delete=%d\n",bias_read,
      bias_create);
   fprintf(fp,"%ssing Unix buffered file I/O\n",buffered_io?"U":"Not u");
   fprintf(fp,"Random number generator seed is %d\n",seed);

   fprintf(fp,"Report format is %s.\n",report?"terse":"verbose");

   if (param && fp!=stdout)
      fclose(fp);

   return(1); /* return 1 unless exit requested, then return 0 */
}

/* CLI callback for 'quit' - returns 0 causing UI to exit */
int cli_quit(param) /* none */
char *param; /* unused */
{
   return(0); /* return 1 unless exit requested, then return 0 */
}

/* CLI callback for 'help' - prints help strings from command_list */
int cli_help(param)
char *param; /* optional: specific command to get help for */
{
   int n=0; /* number of matching items */
   int i; /* traversal variable for command table */
   int len;

   if (param && (len=strlen(param))>0) /* if a command is specified... */
      for (i=0; command_list[i].name; i++) /* walk command table */
         if (!strncmp(command_list[i].name,param,len))
            {
            printf("%s - %s\n",command_list[i].name,command_list[i].help);
            n++;
            }

   if (!param || !n)
      for (i=0; command_list[i].name; i++) /* traverse command table */
         printf("%s - %s\n",command_list[i].name,command_list[i].help);

   return(1); /* return 1 unless exit requested, then return 0 */
}

/* read CLI line from user, translate aliases if any, return fgets status */
char *cli_read_line(buffer,size)
char *buffer; /* empty input line */
int size;
{
   char *result;

   printf("%s",PROMPT);                 /* print prompt */
   fflush(stdout);                      /* force prompt to print */
   if (result=fgets(buffer,size,stdin)) /* read line safely */
      {
      buffer[strlen(buffer)-1]='\0';    /* delete final CR */
      if (!strcmp(buffer,"?"))           /* translate aliases */
         strcpy(buffer,"help");
      if (!strcmp(buffer,"exit"))
         strcpy(buffer,"quit");
      }

   return(result);                      /* return success of fgets */
}

/* parse CLI input line */
int cli_parse_line(buffer)
char *buffer; /* line of user input */
{
   int result=1; /* default return status */
   int len; /* length of parsed command */
   int i; /* traversal variable for command table */

   if (*buffer=='!') /* check for shell escape */
      system((strlen(buffer)>1)?buffer+1:getenv("SHELL"));
   else
      {
      for (i=0; command_list[i].name; i++) /* walk command table */
         if (!strncmp(command_list[i].name,buffer,
            len=strlen(command_list[i].name)))
            { /* if command matches... */
            result=(command_list[i].func)
               (((int)strlen(buffer)>len)?buffer+len+1:NULL);
            break; /* call function and pass remainder of line as parameter */
            }

      if (!command_list[i].name) /* if no commands were called... */
         printf("Eh?\n"); /* tribute to Canadian diction */
      }

   return(result); /* return 1 unless exit requested, then return 0 */
}

/* read config file if present and process it line by line
   - if 'quit' is in file then function returns 0 */
int read_config_file(filename,buffer,ignore)
char *filename; /* file name of config file */
char *buffer;   /* temp storage for each line read from file */
int ignore;     /* ignore file not found */
{
   int result=1; /* default exit value - proceed with UI */
   FILE *fp;

   if (fp=fopen(filename,"r")) /* open config file */
      {
      printf("Reading configuration from file '%s'\n",filename);
      while (fgets(buffer,MAX_LINE,fp) && result) /* read lines until 'quit' */
         {
         buffer[strlen(buffer)-1]='\0'; /* delete final CR */
         result=cli_parse_line(buffer); /* process line as typed in */
         }

      fclose(fp);
      }
   else
      if (!ignore)
         fprintf(stderr,"Error: cannot read configuration file '%s'\n",
            filename);

   return(result);
}

/* main function - reads config files then enters get line/parse line loop */
main(argc,argv)
int argc;
char *argv[];
{
   char buffer[MAX_LINE+1]; /* storage for input command line */

   printf("PostMark %s\n",PM_VERSION);
   if (read_config_file((argc==2)?argv[1]:".pmrc",buffer,1))
      while (cli_read_line(buffer,MAX_LINE) && cli_parse_line(buffer))
         ;
}

/*

                         The "Artistic License"

                                Preamble

The intent of this document is to state the conditions under which a
Package may be copied, such that the Copyright Holder maintains some
semblance of artistic control over the development of the package,
while giving the users of the package the right to use and distribute
the Package in a more-or-less customary fashion, plus the right to make
reasonable modifications.

Definitions:

        "Package" refers to the collection of files distributed by the
        Copyright Holder, and derivatives of that collection of files
        created through textual modification.

        "Standard Version" refers to such a Package if it has not been
        modified, or has been modified in accordance with the wishes
        of the Copyright Holder as specified below.

        "Copyright Holder" is whoever is named in the copyright or
        copyrights for the package.

        "You" is you, if you're thinking about copying or distributing
        this Package.

        "Reasonable copying fee" is whatever you can justify on the
        basis of media cost, duplication charges, time of people involved,
        and so on.  (You will not be required to justify it to the
        Copyright Holder, but only to the computing community at large
        as a market that must bear the fee.)

        "Freely Available" means that no fee is charged for the item
        itself, though there may be fees involved in handling the item.
        It also means that recipients of the item may redistribute it
        under the same conditions they received it.

1. You may make and give away verbatim copies of the source form of the
Standard Version of this Package without restriction, provided that you
duplicate all of the original copyright notices and associated disclaimers.

2. You may apply bug fixes, portability fixes and other modifications
derived from the Public Domain or from the Copyright Holder.  A Package
modified in such a way shall still be considered the Standard Version.

3. You may otherwise modify your copy of this Package in any way, provided
that you insert a prominent notice in each changed file stating how and
when you changed that file, and provided that you do at least ONE of the
following:

    a) place your modifications in the Public Domain or otherwise make them
    Freely Available, such as by posting said modifications to Usenet or
    an equivalent medium, or placing the modifications on a major archive
    site such as uunet.uu.net, or by allowing the Copyright Holder to include
    your modifications in the Standard Version of the Package.

    b) use the modified Package only within your corporation or organization.

    c) rename any non-standard executables so the names do not conflict
    with standard executables, which must also be provided, and provide
    a separate manual page for each non-standard executable that clearly
    documents how it differs from the Standard Version.

    d) make other distribution arrangements with the Copyright Holder.

4. You may distribute the programs of this Package in object code or
executable form, provided that you do at least ONE of the following:

    a) distribute a Standard Version of the executables and library files,
    together with instructions (in the manual page or equivalent) on where
    to get the Standard Version.

    b) accompany the distribution with the machine-readable source of
    the Package with your modifications.

    c) give non-standard executables non-standard names, and clearly
    document the differences in manual pages (or equivalent), together
    with instructions on where to get the Standard Version.

    d) make other distribution arrangements with the Copyright Holder.

5. You may charge a reasonable copying fee for any distribution of this
Package.  You may charge any fee you choose for support of this
Package.  You may not charge a fee for this Package itself.  However,
you may distribute this Package in aggregate with other (possibly
commercial) programs as part of a larger (possibly commercial) software
distribution provided that you do not advertise this Package as a
product of your own.  You may embed this Package's interpreter within
an executable of yours (by linking); this shall be construed as a mere
form of aggregation, provided that the complete Standard Version of the
interpreter is so embedded.

6. The scripts and library files supplied as input to or produced as
output from the programs of this Package do not automatically fall
under the copyright of this Package, but belong to whomever generated
them, and may be sold commercially, and may be aggregated with this
Package.  If such scripts or library files are aggregated with this
Package via the so-called "undump" or "unexec" methods of producing a
binary executable image, then distribution of such an image shall
neither be construed as a distribution of this Package nor shall it
fall under the restrictions of Paragraphs 3 and 4, provided that you do
not represent such an executable image as a Standard Version of this
Package.

7. C subroutines (or comparably compiled subroutines in other
languages) supplied by you and linked into this Package in order to
emulate subroutines and variables of the language defined by this
Package shall not be considered part of this Package, but are the
equivalent of input as in Paragraph 6, provided these subroutines do
not change the language in any way that would cause it to fail the
regression tests for the language.

8. Aggregation of this Package with a commercial distribution is always
permitted provided that the use of this Package is embedded; that is,
when no overt attempt is made to make this Package's interfaces visible
to the end user of the commercial distribution.  Such use shall not be
construed as a distribution of this Package.

9. The name of the Copyright Holder may not be used to endorse or promote
products derived from this software without specific prior written permission.

10. THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.

                                The End

*/


/* A C-program for MT19937: Integer version (1999/10/28)          */
/*  genrand() generates one pseudorandom unsigned integer (32bit) */
/* which is uniformly distributed among 0 to 2^32-1  for each     */
/* call. sgenrand(seed) sets initial values to the working area   */
/* of 624 words. Before genrand(), sgenrand(seed) must be         */
/* called once. (seed is any 32-bit integer.)                     */
/*   Coded by Takuji Nishimura, considering the suggestions by    */
/* Topher Cooper and Marc Rieffel in July-Aug. 1997.              */

/* This library is free software; you can redistribute it and/or   */
/* modify it under the terms of the GNU Library General Public     */
/* License as published by the Free Software Foundation; either    */
/* version 2 of the License, or (at your option) any later         */
/* version.                                                        */
/* This library is distributed in the hope that it will be useful, */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of  */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.            */
/* See the GNU Library General Public License for more details.    */
/* You should have received a copy of the GNU Library General      */
/* Public License along with this library; if not, write to the    */
/* Free Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA   */
/* 02111-1307  USA                                                 */

/* Copyright (C) 1997, 1999 Makoto Matsumoto and Takuji Nishimura. */
/* Any feedback is very welcome. For any question, comments,       */
/* see http://www.math.keio.ac.jp/matumoto/emt.html or email       */
/* matumoto@math.keio.ac.jp                                        */

/* REFERENCE                                                       */
/* M. Matsumoto and T. Nishimura,                                  */
/* "Mersenne Twister: A 623-Dimensionally Equidistributed Uniform  */
/* Pseudo-Random Number Generator",                                */
/* ACM Transactions on Modeling and Computer Simulation,           */
/* Vol. 8, No. 1, January 1998, pp 3--30.                          */

/* Period parameters */
#define N 624
#define M 397
#define MATRIX_A 0x9908b0df   /* constant vector a */
#define UPPER_MASK 0x80000000 /* most significant w-r bits */
#define LOWER_MASK 0x7fffffff /* least significant r bits */

/* Tempering parameters */
#define TEMPERING_MASK_B 0x9d2c5680
#define TEMPERING_MASK_C 0xefc60000
#define TEMPERING_SHIFT_U(y)  (y >> 11)
#define TEMPERING_SHIFT_S(y)  (y << 7)
#define TEMPERING_SHIFT_T(y)  (y << 15)
#define TEMPERING_SHIFT_L(y)  (y >> 18)

static unsigned long mt[N]; /* the array for the state vector  */
static int mti=N+1; /* mti==N+1 means mt[N] is not initialized */

/* Initializing the array with a seed */
void
sgenrand(seed)
    unsigned long seed;
{
    int i;

    for (i=0;i<N;i++) {
         mt[i] = seed & 0xffff0000;
         seed = 69069 * seed + 1;
         mt[i] |= (seed & 0xffff0000) >> 16;
         seed = 69069 * seed + 1;
    }
    mti = N;
}

/* Initialization by "sgenrand()" is an example. Theoretically,      */
/* there are 2^19937-1 possible states as an intial state.           */
/* This function allows to choose any of 2^19937-1 ones.             */
/* Essential bits in "seed_array[]" is following 19937 bits:         */
/*  (seed_array[0]&UPPER_MASK), seed_array[1], ..., seed_array[N-1]. */
/* (seed_array[0]&LOWER_MASK) is discarded.                          */
/* Theoretically,                                                    */
/*  (seed_array[0]&UPPER_MASK), seed_array[1], ..., seed_array[N-1]  */
/* can take any values except all zeros.                             */
void
lsgenrand(seed_array)
    unsigned long seed_array[];
    /* the length of seed_array[] must be at least N */
{
    int i;

    for (i=0;i<N;i++)
      mt[i] = seed_array[i];
    mti=N;
}

unsigned long
genrand()
{
    unsigned long y;
    static unsigned long mag01[2]={0x0, MATRIX_A};
    /* mag01[x] = x * MATRIX_A  for x=0,1 */

    if (mti >= N) { /* generate N words at one time */
        int kk;

        if (mti == N+1)   /* if sgenrand() has not been called, */
            sgenrand(4357); /* a default initial seed is used   */

        for (kk=0;kk<N-M;kk++) {
            y = (mt[kk]&UPPER_MASK)|(mt[kk+1]&LOWER_MASK);
            mt[kk] = mt[kk+M] ^ (y >> 1) ^ mag01[y & 0x1];
        }
        for (;kk<N-1;kk++) {
            y = (mt[kk]&UPPER_MASK)|(mt[kk+1]&LOWER_MASK);
            mt[kk] = mt[kk+(M-N)] ^ (y >> 1) ^ mag01[y & 0x1];
        }
        y = (mt[N-1]&UPPER_MASK)|(mt[0]&LOWER_MASK);
        mt[N-1] = mt[M-1] ^ (y >> 1) ^ mag01[y & 0x1];

        mti = 0;
    }

    y = mt[mti++];
    y ^= TEMPERING_SHIFT_U(y);
    y ^= TEMPERING_SHIFT_S(y) & TEMPERING_MASK_B;
    y ^= TEMPERING_SHIFT_T(y) & TEMPERING_MASK_C;
    y ^= TEMPERING_SHIFT_L(y);

    return y;
}
