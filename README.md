# ImageDownloader_Thread

This project is intended to demonstrate an image download process by using a Runnable task that runs inside a thread. 
It also handles configuration changes that is useful in these two cases:
  - If it happens during the download process, it will save/restore progress bar with the the image file name.
  - If it happens after the download finishes, it will save/restore the bitmap.
  
The communication between the service and the main UI thread uses two different approaches:
  -  A Handler component.
  -  A reference to a retained fragment (which is not destroyed during configuration changes). 

Please, refer to [this article](http://androidahead.com/2017/02/11/using-threads-in-android-and-communicating-them-with-the-ui-thread/) for detailed information.

![Demo](https://cloud.githubusercontent.com/assets/4574670/22719707/e903e326-ed8d-11e6-98a7-0f05fa4d421b.gif)

# License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details



