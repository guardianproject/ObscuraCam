#ifndef INCLUDE_REDACTION
#define INCLUDE_REDACTION

namespace jpeg_redaction {
// Class to store information redacted from a horizontal strip of image.
class JpegStrip {
public:
  JpegStrip(int x, int y) : x_(x), y_(y) {
    blocks_ = 0;
    bits_ = 0;
  }
  int AppendBlock(const unsigned char* data, int bits) {
    data_.resize((bits_ + bits + 7)/8);
    // copy over bits.

    bits_ += bits;
    return ++blocks_;
  }
  std::string data_; // Raw binary encoded data.
  int bits_;
  int x_; // Coordinate of start (from left)
  int y_; // Coordinate of start (from top)
  int blocks_; // Number of blocks stored.
};

// Class to define the areas to be redacted, and return the redacted
// information.
class Redaction {
public:
  // Simple rectangle class for redaction regions.
  class Rect {
   public:
    Rect(int l, int r, int t, int b) : l_(l), r_(r), t_(t), b_(b) {}
    int l_, r_, t_, b_;
  };
  Redaction() {};
  void AddRegion(const Rect &r) {
    regions_.push_back(r);
  }
  void Add(const Redaction &red) {
    for (int i = 0; i < red.NumRegions(); ++i)
      regions_.push_back(red.GetRegion(i));
  }
  Rect GetRegion(int i) const {
    return regions_[i];
  }
  int NumRegions() const {
    return regions_.size();
  }
  void Clear() {
    regions_.clear();
  }
  // Test if a box of width dx, dy, with top left corner at x,y
  // intersects with any of the rectangular regions.
  bool InRegion(int x, int y, int dx, int dy) const {
    for (int i = 0; i < regions_.size(); ++i)
      if (x      < regions_[i].r_ &&
          x + dx > regions_[i].l_ &&
          y      < regions_[i].b_ &&
          y + dy > regions_[i].t_) {
        return true;
      }
    return false;
  }
  // Information redacted.
  std::vector<JpegStrip> strips_;
  std::vector<Rect> regions_;
};
} // namespace jpeg_redaction
#endif // INCLUDE_REDACTION
