# Keep system commands ahead of persistent custom binaries.
case ":$PATH:" in
    *:/data/bin:*) ;;
    *) PATH="${PATH:+$PATH:}/data/bin" ;;
esac
export PATH
